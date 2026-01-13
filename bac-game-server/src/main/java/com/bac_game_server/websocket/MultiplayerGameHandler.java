package com.bac_game_server.websocket;

import com.bac_game_server.dto.*;
import com.bac_game_server.service.GameSessionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * WebSocket handler for multiplayer game coordination.
 * Manages real-time communication for synchronized gameplay, scoring, and leaderboards.
 */
@Component
public class MultiplayerGameHandler implements WebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(MultiplayerGameHandler.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Session management
    private final Map<String, List<WebSocketSession>> sessionConnections = new ConcurrentHashMap<>();
    private final Map<WebSocketSession, String> connectionSessions = new ConcurrentHashMap<>();
    private final Map<WebSocketSession, String> connectionPlayers = new ConcurrentHashMap<>();
    
    // Game state tracking
    private final Map<String, GameRoundState> gameStates = new ConcurrentHashMap<>();
    private final GameSessionService gameSessionService;

    @Autowired
    public MultiplayerGameHandler(GameSessionService gameSessionService) {
        this.gameSessionService = gameSessionService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.info("WebSocket connection established: {}", session.getId());
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String payload = message.getPayload().toString();
        logger.info("Received message from {}: {}", session.getId(), payload);

        try {
            JsonNode messageNode = objectMapper.readTree(payload);
            String type = messageNode.get("type").asText();
            
            switch (type) {
                case "JOIN_SESSION":
                    handleJoinSession(session, messageNode);
                    break;
                case "START_GAME":
                    handleStartGame(session, messageNode);
                    break;
                case "SUBMIT_ANSWERS":
                    handleSubmitAnswers(session, messageNode);
                    break;
                case "NEXT_ROUND":
                    handleNextRound(session, messageNode);
                    break;
                case "END_GAME":
                    handleEndGame(session, messageNode);
                    break;
                default:
                    logger.warn("Unknown message type: {}", type);
            }
        } catch (Exception e) {
            logger.error("Error handling message", e);
            sendError(session, "Error processing message: " + e.getMessage());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("WebSocket transport error for session {}", session.getId(), exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        logger.info("WebSocket connection closed: {} - {}", session.getId(), closeStatus);
        
        String sessionId = connectionSessions.remove(session);
        String playerName = connectionPlayers.remove(session);
        
        if (sessionId != null) {
            List<WebSocketSession> connections = sessionConnections.get(sessionId);
            if (connections != null) {
                connections.remove(session);
                if (connections.isEmpty()) {
                    sessionConnections.remove(sessionId);
                    gameStates.remove(sessionId);
                }
                
                // Notify remaining players
                broadcastToSession(sessionId, createPlayerLeftMessage(playerName));
            }
        }
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    // Message handlers
    private void handleJoinSession(WebSocketSession session, JsonNode message) throws IOException {
        String sessionId = message.get("sessionId").asText();
        String playerName = message.get("playerName").asText();
        
        logger.info("Player {} joining session {}", playerName, sessionId);
        
        // Add to session
        sessionConnections.computeIfAbsent(sessionId, k -> new CopyOnWriteArrayList<>()).add(session);
        connectionSessions.put(session, sessionId);
        connectionPlayers.put(session, playerName);
        
        // Send confirmation to player
        ObjectNode response = objectMapper.createObjectNode();
        response.put("type", "SESSION_JOINED");
        response.put("sessionId", sessionId);
        response.put("playerName", playerName);
        session.sendMessage(new TextMessage(response.toString()));
        
        // Notify other players
        ObjectNode notification = objectMapper.createObjectNode();
        notification.put("type", "PLAYER_JOINED");
        notification.put("playerName", playerName);
        broadcastToSession(sessionId, notification, session);
    }

    private void handleStartGame(WebSocketSession session, JsonNode message) throws IOException {
        String sessionId = connectionSessions.get(session);
        String playerName = connectionPlayers.get(session);
        
        logger.info("=== RECEIVED START_GAME MESSAGE ===");
        logger.info("Session ID: {}", sessionId);
        logger.info("Player: {}", playerName);
        logger.info("Message: {}", message.toString());
        
        if (sessionId == null) {
            logger.error("START_GAME rejected: Not connected to a session");
            sendError(session, "Not connected to a session");
            return;
        }
        
        // Extract game configuration
        JsonNode config = message.get("config");
        if (config == null) {
            logger.error("START_GAME rejected: No config provided");
            sendError(session, "No game configuration provided");
            return;
        }
        
        int numberOfRounds = config.get("numberOfRounds").asInt();
        int roundDuration = config.get("roundDuration").asInt();
        JsonNode categoriesArray = config.get("categories");
        
        List<String> categories = new ArrayList<>();
        for (JsonNode cat : categoriesArray) {
            categories.add(cat.asText());
        }
        
        logger.info("Game config - Rounds: {}, Duration: {}s, Categories: {}", 
                   numberOfRounds, roundDuration, categories);
        
        // Generate first letter
        String letter = generateRandomLetter();
        
        // Initialize game state
        GameRoundState gameState = new GameRoundState(sessionId, numberOfRounds, roundDuration, categories);
        gameState.setCurrentLetter(letter);
        gameState.setCurrentRound(1);
        gameStates.put(sessionId, gameState);
        
        logger.info("Starting game for session {} with {} rounds, {} duration, letter {}", 
                   sessionId, numberOfRounds, roundDuration, letter);
        
        // Broadcast GAME_STARTED to all players
        ObjectNode gameStarted = objectMapper.createObjectNode();
        gameStarted.put("type", "GAME_STARTED");
        gameStarted.put("letter", letter);
        gameStarted.put("roundDuration", roundDuration);
        gameStarted.put("currentRound", 1);
        gameStarted.put("totalRounds", numberOfRounds);
        gameStarted.set("categories", objectMapper.valueToTree(categories));
        
        logger.info("Broadcasting GAME_STARTED to all players in session {}: {}", sessionId, gameStarted.toString());
        broadcastToSession(sessionId, gameStarted);
        logger.info("GAME_STARTED broadcast completed for session {}", sessionId);
        
        // Start round timer
        startRoundTimer(sessionId, roundDuration * 1000);
    }

    private void handleSubmitAnswers(WebSocketSession session, JsonNode message) throws IOException {
        String sessionId = connectionSessions.get(session);
        String playerName = connectionPlayers.get(session);
        
        if (sessionId == null || playerName == null) {
            sendError(session, "Not connected to a session");
            return;
        }
        
        GameRoundState gameState = gameStates.get(sessionId);
        if (gameState == null) {
            sendError(session, "Game not started");
            return;
        }
        
        // Extract answers and calculate score
        JsonNode answersNode = message.get("answers");
        Map<String, String> answers = new HashMap<>();
        answersNode.fields().forEachRemaining(entry -> 
            answers.put(entry.getKey(), entry.getValue().asText()));
        
        int score = calculateScore(answers, gameState.getCurrentLetter());
        
        // Store player results
        gameState.addPlayerResult(playerName, score, answers);
        
        logger.info("Player {} submitted answers for session {}, score: {}", 
                   playerName, sessionId, score);
        
        // Check if all players submitted
        List<WebSocketSession> sessions = sessionConnections.get(sessionId);
        if (sessions != null && gameState.getPlayerResults().size() >= sessions.size()) {
            endRound(sessionId);
        }
    }

    private void handleNextRound(WebSocketSession session, JsonNode message) throws IOException {
        String sessionId = connectionSessions.get(session);
        
        if (sessionId == null) {
            sendError(session, "Not connected to a session");
            return;
        }
        
        GameRoundState gameState = gameStates.get(sessionId);
        if (gameState == null) {
            sendError(session, "Game not found");
            return;
        }
        
        if (gameState.getCurrentRound() >= gameState.getTotalRounds()) {
            // Game finished - show leaderboard
            endGame(sessionId);
        } else {
            // Start next round
            startNextRound(sessionId);
        }
    }

    private void handleEndGame(WebSocketSession session, JsonNode message) throws IOException {
        String sessionId = connectionSessions.get(session);
        
        if (sessionId == null) {
            sendError(session, "Not connected to a session");
            return;
        }
        
        endGame(sessionId);
    }

    // Game logic methods
    private void startRoundTimer(String sessionId, int durationMs) {
        // Use a separate thread for timing to avoid blocking
        new Thread(() -> {
            try {
                Thread.sleep(durationMs);
                endRound(sessionId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void endRound(String sessionId) {
        GameRoundState gameState = gameStates.get(sessionId);
        if (gameState == null) return;
        
        logger.info("Ending round {} for session {}", gameState.getCurrentRound(), sessionId);
        
        // Calculate scores and rankings
        List<PlayerScoreDTO> roundResults = calculateRoundResults(gameState);
        
        // Broadcast ROUND_ENDED
        ObjectNode roundEnded = objectMapper.createObjectNode();
        roundEnded.put("type", "ROUND_ENDED");
        roundEnded.put("currentRound", gameState.getCurrentRound());
        roundEnded.put("totalRounds", gameState.getTotalRounds());
        roundEnded.set("results", objectMapper.valueToTree(roundResults));
        roundEnded.put("isGameFinished", gameState.getCurrentRound() >= gameState.getTotalRounds());
        
        broadcastToSession(sessionId, roundEnded);
    }

    private void startNextRound(String sessionId) {
        GameRoundState gameState = gameStates.get(sessionId);
        if (gameState == null) return;
        
        // Increment round
        gameState.setCurrentRound(gameState.getCurrentRound() + 1);
        gameState.clearRoundResults();
        
        // Generate new letter
        String newLetter = generateRandomLetter();
        gameState.setCurrentLetter(newLetter);
        
        logger.info("Starting round {} for session {} with letter {}", 
                   gameState.getCurrentRound(), sessionId, newLetter);
        
        // Broadcast ROUND_STARTED
        ObjectNode roundStarted = objectMapper.createObjectNode();
        roundStarted.put("type", "ROUND_STARTED");
        roundStarted.put("letter", newLetter);
        roundStarted.put("currentRound", gameState.getCurrentRound());
        roundStarted.put("totalRounds", gameState.getTotalRounds());
        roundStarted.put("roundDuration", gameState.getRoundDuration());
        
        broadcastToSession(sessionId, roundStarted);
        
        // Start timer
        startRoundTimer(sessionId, gameState.getRoundDuration() * 1000);
    }

    private void endGame(String sessionId) {
        GameRoundState gameState = gameStates.get(sessionId);
        if (gameState == null) return;
        
        logger.info("Ending game for session {}", sessionId);
        
        // Calculate final leaderboard
        List<PlayerScoreDTO> leaderboard = calculateFinalLeaderboard(gameState);
        
        // Broadcast GAME_ENDED
        ObjectNode gameEnded = objectMapper.createObjectNode();
        gameEnded.put("type", "GAME_ENDED");
        gameEnded.set("leaderboard", objectMapper.valueToTree(leaderboard));
        
        broadcastToSession(sessionId, gameEnded);
        
        // Clean up game state
        gameStates.remove(sessionId);
    }

    // Utility methods
    private String generateRandomLetter() {
        String validLetters = "ABCDEFGHIJKLMNOPQRSTUVWYZ"; // Excluding X
        Random random = new Random();
        return String.valueOf(validLetters.charAt(random.nextInt(validLetters.length())));
    }

    private int calculateScore(Map<String, String> answers, String letter) {
        int score = 0;
        for (Map.Entry<String, String> entry : answers.entrySet()) {
            String answer = entry.getValue();
            if (answer != null && !answer.trim().isEmpty() && 
                answer.toUpperCase().startsWith(letter.toUpperCase())) {
                score += 10; // Base score for valid answer
            }
        }
        return score;
    }

    private List<PlayerScoreDTO> calculateRoundResults(GameRoundState gameState) {
        List<PlayerScoreDTO> results = new ArrayList<>();
        Map<String, GameRoundState.PlayerResult> playerResults = gameState.getPlayerResults();
        
        // Sort by round score descending
        List<Map.Entry<String, GameRoundState.PlayerResult>> sortedResults = 
            playerResults.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().getRoundScore(), a.getValue().getRoundScore()))
                .collect(Collectors.toList());
        
        int rank = 1;
        for (Map.Entry<String, GameRoundState.PlayerResult> entry : sortedResults) {
            String playerName = entry.getKey();
            GameRoundState.PlayerResult result = entry.getValue();
            
            // Update total score
            gameState.updatePlayerTotalScore(playerName, result.getRoundScore());
            
            results.add(new PlayerScoreDTO(
                playerName,
                gameState.getPlayerTotalScore(playerName),
                result.getRoundScore(),
                result.getAnswers(),
                rank++
            ));
        }
        
        return results;
    }

    private List<PlayerScoreDTO> calculateFinalLeaderboard(GameRoundState gameState) {
        List<PlayerScoreDTO> leaderboard = new ArrayList<>();
        Map<String, Integer> totalScores = gameState.getPlayerTotalScores();
        
        // Sort by total score descending
        List<Map.Entry<String, Integer>> sortedScores = 
            totalScores.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toList());
        
        int rank = 1;
        for (Map.Entry<String, Integer> entry : sortedScores) {
            leaderboard.add(new PlayerScoreDTO(
                entry.getKey(),
                entry.getValue(),
                0, // No round score for final leaderboard
                new HashMap<>(), // No answers for final leaderboard
                rank++
            ));
        }
        
        return leaderboard;
    }

    private void broadcastToSession(String sessionId, ObjectNode message) {
        broadcastToSession(sessionId, message, null);
    }

    private void broadcastToSession(String sessionId, ObjectNode message, WebSocketSession exclude) {
        List<WebSocketSession> sessions = sessionConnections.get(sessionId);
        logger.info("Broadcasting message type {} to session {} ({} total sessions)", 
                   message.get("type"), sessionId, sessions != null ? sessions.size() : 0);
        
        if (sessions != null) {
            TextMessage textMessage = new TextMessage(message.toString());
            int successCount = 0;
            for (WebSocketSession session : sessions) {
                if (!session.equals(exclude) && session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                        successCount++;
                        logger.debug("Sent message to session {}", session.getId());
                    } catch (IOException e) {
                        logger.error("Error sending message to session {}", session.getId(), e);
                    }
                } else {
                    logger.debug("Skipped session {} (excluded: {}, open: {})", 
                               session.getId(), session.equals(exclude), session.isOpen());
                }
            }
            logger.info("Message broadcast completed: {} out of {} sessions received the message", 
                       successCount, sessions.size());
        } else {
            logger.warn("No sessions found for session ID {}", sessionId);
        }
    }

    private void sendError(WebSocketSession session, String message) {
        try {
            ObjectNode error = objectMapper.createObjectNode();
            error.put("type", "ERROR");
            error.put("message", message);
            session.sendMessage(new TextMessage(error.toString()));
        } catch (IOException e) {
            logger.error("Error sending error message", e);
        }
    }

    private ObjectNode createPlayerLeftMessage(String playerName) {
        ObjectNode message = objectMapper.createObjectNode();
        message.put("type", "PLAYER_LEFT");
        message.put("playerName", playerName);
        return message;
    }
}
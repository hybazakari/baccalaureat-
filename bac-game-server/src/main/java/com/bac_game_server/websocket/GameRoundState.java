package com.bac_game_server.websocket;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the state of a multiplayer game session including rounds, scores, and timing.
 */
public class GameRoundState {
    private final String sessionId;
    private final int totalRounds;
    private final int roundDuration; // seconds
    private final List<String> categories;
    
    private int currentRound;
    private String currentLetter;
    private final Map<String, PlayerResult> playerResults = new ConcurrentHashMap<>();
    private final Map<String, Integer> playerTotalScores = new ConcurrentHashMap<>();

    public GameRoundState(String sessionId, int totalRounds, int roundDuration, List<String> categories) {
        this.sessionId = sessionId;
        this.totalRounds = totalRounds;
        this.roundDuration = roundDuration;
        this.categories = categories;
        this.currentRound = 1;
    }

    // Player result tracking
    public static class PlayerResult {
        private final int roundScore;
        private final Map<String, String> answers;

        public PlayerResult(int roundScore, Map<String, String> answers) {
            this.roundScore = roundScore;
            this.answers = new HashMap<>(answers);
        }

        public int getRoundScore() { return roundScore; }
        public Map<String, String> getAnswers() { return answers; }
    }

    // Getters and setters
    public String getSessionId() { return sessionId; }
    public int getTotalRounds() { return totalRounds; }
    public int getRoundDuration() { return roundDuration; }
    public List<String> getCategories() { return categories; }
    
    public int getCurrentRound() { return currentRound; }
    public void setCurrentRound(int currentRound) { this.currentRound = currentRound; }
    
    public String getCurrentLetter() { return currentLetter; }
    public void setCurrentLetter(String currentLetter) { this.currentLetter = currentLetter; }
    
    public Map<String, PlayerResult> getPlayerResults() { return playerResults; }
    public Map<String, Integer> getPlayerTotalScores() { return playerTotalScores; }
    
    // Player management methods
    public void addPlayerResult(String playerName, int roundScore, Map<String, String> answers) {
        playerResults.put(playerName, new PlayerResult(roundScore, answers));
    }
    
    public void updatePlayerTotalScore(String playerName, int additionalScore) {
        playerTotalScores.merge(playerName, additionalScore, Integer::sum);
    }
    
    public int getPlayerTotalScore(String playerName) {
        return playerTotalScores.getOrDefault(playerName, 0);
    }
    
    public void clearRoundResults() {
        playerResults.clear();
    }
}
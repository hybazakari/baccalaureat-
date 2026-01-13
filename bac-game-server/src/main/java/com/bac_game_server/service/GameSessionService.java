package com.bac_game_server.service;

import com.bac_game_server.entity.GameSessionEntity;
import com.bac_game_server.entity.GameSessionStatus;
import com.bac_game_server.entity.PlayerEntity;
import com.bac_game_server.entity.PlayerSessionEntity;
import com.bac_game_server.exception.RoomNotFoundException;
import com.bac_game_server.exception.RoomNotJoinableException;
import com.bac_game_server.repository.GameSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service class responsible for multiplayer word game session management.
 * Enforces server as single source of truth for all game logic.
 */
@Service
@Transactional
public class GameSessionService {

    private static final Logger logger = LoggerFactory.getLogger(GameSessionService.class);
    private static final int SESSION_ID_LENGTH = 6;
    private static final String SESSION_ID_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int MAX_SESSION_GENERATION_ATTEMPTS = 10;
    
    private final GameSessionRepository gameSessionRepository;
    private final PlayerSessionService playerSessionService;
    private final PlayerService playerService;
    private final SecureRandom random;

    @Autowired
    public GameSessionService(GameSessionRepository gameSessionRepository,
                             PlayerSessionService playerSessionService,
                             PlayerService playerService) {
        this.gameSessionRepository = gameSessionRepository;
        this.playerSessionService = playerSessionService;
        this.playerService = playerService;
        this.random = new SecureRandom();
    }

    /**
     * Create a new multiplayer session with host as first player.
     * Server is single source of truth for sessionId generation.
     * 
     * @param hostUsername the username of the session host
     * @param categories list of game categories
     * @param roundDuration duration of each round in seconds
     * @return the created game session
     */
    public GameSessionEntity createSession(String hostUsername, List<String> categories, int roundDuration) {
        logger.info("Creating new session - host: {}, categories: {}, duration: {}", 
                   hostUsername, categories, roundDuration);

        // Get or create the host player
        PlayerEntity host = playerService.getOrCreatePlayer(hostUsername);
        
        // Generate unique session ID
        String sessionId = generateUniqueSessionId();
        
        // Create the session
        GameSessionEntity session = new GameSessionEntity(sessionId, hostUsername, categories, roundDuration);
        session = gameSessionRepository.save(session);
        
        // Add host as first player
        playerSessionService.createSession(host, session, true);
        
        logger.info("Session created successfully - sessionId: {}, host: {}", sessionId, hostUsername);
        return session;
    }

    /**
     * Allow a player to join an existing session using sessionId.
     * SessionId is the only way to join a game.
     * 
     * @param sessionId the session ID to join
     * @param playerUsername the player's username
     * @return the updated game session
     */
    public GameSessionEntity joinSession(String sessionId, String playerUsername) {
        logger.info("Player joining session - sessionId: {}, player: {}", sessionId, playerUsername);

        // Find the session
        GameSessionEntity session = findBySessionId(sessionId)
                .orElseThrow(() -> new RoomNotFoundException(sessionId));

        // Check if session can accept new players
        if (!session.canAcceptNewPlayers()) {
            logger.warn("Session not joinable - sessionId: {}, status: {}", sessionId, session.getStatus());
            throw new RoomNotJoinableException(sessionId, session.getStatus().name());
        }

        // Get or create the player
        PlayerEntity player = playerService.getOrCreatePlayer(playerUsername);

        // Check if player is already in this session
        if (playerSessionService.isPlayerInSession(player, session)) {
            logger.warn("Player already in session - sessionId: {}, player: {}", sessionId, playerUsername);
            throw new IllegalArgumentException("Player " + playerUsername + " is already in session " + sessionId);
        }

        // Add player to session
        playerSessionService.createSession(player, session, false);
        
        logger.info("Player joined successfully - sessionId: {}, player: {}", sessionId, playerUsername);
        return session;
    }

    /**
     * Start a game round for a session.
     * Server generates the letter - single source of truth.
     * 
     * @param sessionId the session ID
     * @return the updated session with generated letter and timing
     */
    public GameSessionEntity startRound(String sessionId) {
        logger.info("Starting round for session: {}", sessionId);

        GameSessionEntity session = findBySessionId(sessionId)
                .orElseThrow(() -> new RoomNotFoundException(sessionId));

        if (session.getStatus() != GameSessionStatus.WAITING) {
            throw new IllegalStateException("Cannot start round for session " + sessionId + 
                    " - current status is " + session.getStatus());
        }

        // Server generates the letter (single source of truth)
        session.startRound();
        session = gameSessionRepository.save(session);

        logger.info("Round started - sessionId: {}, letter: {}, duration: {}", 
                   sessionId, session.getLetter(), session.getRoundDuration());
        return session;
    }

    /**
     * Start a round with configuration parameters.
     * Server generates the letter and applies configuration.
     * 
     * @param sessionId the session ID
     * @param numberOfRounds the number of rounds
     * @param roundDuration the round duration in seconds
     * @param categories the list of categories
     * @return the updated session with generated letter and configuration
     */
    public GameSessionEntity startRoundWithConfig(String sessionId, int numberOfRounds, 
                                                 int roundDuration, List<String> categories) {
        logger.info("Starting round with config for session: {}, rounds: {}, duration: {}, categories: {}", 
                   sessionId, numberOfRounds, roundDuration, categories);

        GameSessionEntity session = findBySessionId(sessionId)
                .orElseThrow(() -> new RoomNotFoundException(sessionId));

        if (session.getStatus() != GameSessionStatus.WAITING) {
            throw new IllegalStateException("Cannot start round for session " + sessionId + 
                    " - current status is " + session.getStatus());
        }

        // Apply configuration before starting
        session.setRoundDuration(roundDuration);
        session.setCategories(categories);
        
        // Server generates the letter (single source of truth)
        session.startRound();
        session = gameSessionRepository.save(session);

        logger.info("Round started with config - sessionId: {}, letter: {}, duration: {}, categories: {}", 
                   sessionId, session.getLetter(), session.getRoundDuration(), categories.size());
        return session;
    }

    /**
     * Submit results for a player in a session.
     * Server will validate and score the results.
     * 
     * @param sessionId the session ID
     * @param playerUsername the player's username
     * @param results the player's answers (category -> answer)
     * @return true if submission was successful
     */
    public boolean submitResults(String sessionId, String playerUsername, Map<String, String> results) {
        logger.info("Results submission - sessionId: {}, player: {}, answers: {}", 
                   sessionId, playerUsername, results.size());

        GameSessionEntity session = findBySessionId(sessionId)
                .orElseThrow(() -> new RoomNotFoundException(sessionId));

        PlayerEntity player = playerService.findByUsername(playerUsername)
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + playerUsername));

        // Submit results through player session service
        boolean success = playerSessionService.submitResults(player, session, results);
        
        if (success) {
            logger.info("Results submitted successfully - sessionId: {}, player: {}", sessionId, playerUsername);
            
            // Check if all players have submitted
            checkRoundCompletion(session);
        } else {
            logger.warn("Results submission failed - sessionId: {}, player: {}", sessionId, playerUsername);
        }
        
        return success;
    }

    /**
     * Check if all players in a session have submitted their results.
     * If so, end the round and process results.
     * 
     * @param session the game session
     */
    private void checkRoundCompletion(GameSessionEntity session) {
        long totalPlayers = playerSessionService.countPlayersInSession(session);
        long submittedCount = playerSessionService.countSubmittedResultsInSession(session);
        
        logger.info("Round completion check - sessionId: {}, submitted: {}/{}", 
                   session.getSessionId(), submittedCount, totalPlayers);

        if (submittedCount >= totalPlayers) {
            // All players have submitted, end the round
            session.endRound();
            gameSessionRepository.save(session);
            logger.info("Round completed - sessionId: {}", session.getSessionId());
        }
    }

    /**
     * Get the complete game state for a session.
     * 
     * @param sessionId the session ID
     * @return Optional containing the session if found
     */
    @Transactional(readOnly = true)
    public Optional<GameSessionEntity> findBySessionId(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return Optional.empty();
        }
        return gameSessionRepository.findBySessionId(sessionId.trim().toUpperCase());
    }

    /**
     * Generate a unique session ID that doesn't already exist in the database.
     * 
     * @return a unique 6-character session ID
     * @throws RuntimeException if unable to generate unique ID after maximum attempts
     */
    private String generateUniqueSessionId() {
        for (int attempt = 0; attempt < MAX_SESSION_GENERATION_ATTEMPTS; attempt++) {
            String sessionId = generateRandomSessionId();
            if (!gameSessionRepository.existsBySessionId(sessionId)) {
                return sessionId;
            }
        }
        throw new RuntimeException("Unable to generate unique session ID after " + 
                MAX_SESSION_GENERATION_ATTEMPTS + " attempts");
    }

    /**
     * Generate a random 6-character session ID using uppercase letters and numbers.
     * 
     * @return a random 6-character session ID
     */
    private String generateRandomSessionId() {
        StringBuilder sessionId = new StringBuilder(SESSION_ID_LENGTH);
        for (int i = 0; i < SESSION_ID_LENGTH; i++) {
            int randomIndex = random.nextInt(SESSION_ID_CHARACTERS.length());
            sessionId.append(SESSION_ID_CHARACTERS.charAt(randomIndex));
        }
        return sessionId.toString();
    }

    /**
     * Process results for all players in a session and calculate final scores.
     * Server compares and validates all results.
     * 
     * @param sessionId the session ID
     */
    public void processResults(String sessionId) {
        logger.info("Processing results for session: {}", sessionId);

        GameSessionEntity session = findBySessionId(sessionId)
                .orElseThrow(() -> new RoomNotFoundException(sessionId));

        if (session.getStatus() != GameSessionStatus.RESULTS_PENDING) {
            logger.warn("Cannot process results - sessionId: {}, status: {}", sessionId, session.getStatus());
            return;
        }

        // Process results through player session service
        playerSessionService.processAllResults(session, session.getLetter());
        
        // Mark session as finished
        session.finishSession();
        gameSessionRepository.save(session);
        
        logger.info("Results processing completed for session: {}", sessionId);
    }
}
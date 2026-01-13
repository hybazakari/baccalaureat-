package com.bac_game_server.controller;

import com.bac_game_server.dto.*;
import com.bac_game_server.entity.GameSessionEntity;
import com.bac_game_server.entity.PlayerSessionEntity;
import com.bac_game_server.mapper.GameSessionMapper;
import com.bac_game_server.service.GameSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * REST controller for multiplayer word game session operations.
 * Enforces server as single source of truth for all game logic.
 */
@RestController
@RequestMapping("/api/sessions")
@CrossOrigin(origins = "*") // Allow cross-origin requests for development
public class GameSessionController {

    private static final Logger logger = LoggerFactory.getLogger(GameSessionController.class);

    private final GameSessionService gameSessionService;
    private final GameSessionMapper gameSessionMapper;

    @Autowired
    public GameSessionController(GameSessionService gameSessionService,
                                GameSessionMapper gameSessionMapper) {
        this.gameSessionService = gameSessionService;
        this.gameSessionMapper = gameSessionMapper;
    }

    /**
     * Create a new multiplayer session.
     * Host becomes the first player and server generates sessionId.
     * 
     * @param request the create session request
     * @return ResponseEntity containing the session ID and confirmation
     */
    @PostMapping("/create")
    public ResponseEntity<CreateSessionResponse> createSession(@RequestBody CreateSessionRequest request) {
        logger.info("Session creation request received - host: {}, categories: {}, duration: {}", 
                   request.getHostUsername(), request.getCategories(), request.getRoundDuration());

        // Validate request
        if (request == null || request.getHostUsername() == null || 
            request.getHostUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Host username is required");
        }
        
        if (request.getCategories() == null || request.getCategories().isEmpty()) {
            throw new IllegalArgumentException("At least one category is required");
        }
        
        if (request.getRoundDuration() <= 0) {
            throw new IllegalArgumentException("Round duration must be positive");
        }

        // Create the session - server is single source of truth
        GameSessionEntity session = gameSessionService.createSession(
                request.getHostUsername().trim(),
                request.getCategories(),
                request.getRoundDuration()
        );

        // Return session ID - MUST contain sessionId as per contract
        CreateSessionResponse response = new CreateSessionResponse(
                session.getSessionId(), 
                request.getHostUsername(),
                "Session created successfully"
        );
        
        logger.info("Session creation completed - sessionId: {}, host: {}", 
                   session.getSessionId(), request.getHostUsername());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Join an existing session using sessionId.
     * SessionId is the only way to join a game.
     * 
     * @param request the join session request
     * @return ResponseEntity containing the updated game state
     */
    @PostMapping("/join")
    public ResponseEntity<GameStateDTO> joinSession(@RequestBody JoinSessionRequest request) {
        logger.info("Session join request received - sessionId: {}, player: {}", 
                   request.getSessionId(), request.getPlayerUsername());

        // Validate request
        if (request == null || request.getSessionId() == null || 
            request.getSessionId().trim().isEmpty()) {
            throw new IllegalArgumentException("Session ID is required");
        }
        
        if (request.getPlayerUsername() == null || 
            request.getPlayerUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Player username is required");
        }

        // Join the session - server validates everything
        GameSessionEntity session = gameSessionService.joinSession(
                request.getSessionId().trim(),
                request.getPlayerUsername().trim()
        );

        // Return complete game state
        GameStateDTO gameState = gameSessionMapper.toGameStateDTO(session);
        
        logger.info("Session join completed - sessionId: {}, player: {}", 
                   request.getSessionId(), request.getPlayerUsername());

        return ResponseEntity.ok(gameState);
    }

    /**
     * Get complete game state for a session.
     * Returns the same letter and categories for all players.
     * 
     * @param sessionId the session ID
     * @return ResponseEntity containing the game state
     */
    @GetMapping("/{sessionId}/state")
    public ResponseEntity<GameStateDTO> getGameState(@PathVariable String sessionId) {
        logger.info("Game state request - sessionId: {}", sessionId);

        // Validate sessionId
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Session ID is required");
        }

        // Find the session
        Optional<GameSessionEntity> sessionOpt = gameSessionService.findBySessionId(sessionId.trim());
        if (sessionOpt.isEmpty()) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        // Return complete game state with server-generated letter and categories
        GameStateDTO gameState = gameSessionMapper.toGameStateDTO(sessionOpt.get());
        
        logger.info("Game state returned - sessionId: {}, letter: {}, status: {}", 
                   sessionId, gameState.getLetter(), gameState.getStatus());

        return ResponseEntity.ok(gameState);
    }

    /**
     * Start a game round for a session with configuration.
     * Server generates the letter - single source of truth.
     * 
     * @param sessionId the session ID
     * @param request the start game configuration
     * @return ResponseEntity containing the updated game state with letter and timing
     */
    @PostMapping("/{sessionId}/start")
    public ResponseEntity<GameStateDTO> startRound(@PathVariable String sessionId, @RequestBody StartGameRequest request) {
        logger.info("Round start request - sessionId: {}, config: {}", sessionId, request);

        // Validate sessionId
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Session ID is required");
        }
        
        // Validate configuration
        if (request == null) {
            throw new IllegalArgumentException("Game configuration is required");
        }

        // Start the round with configuration - server generates letter
        GameSessionEntity session = gameSessionService.startRoundWithConfig(
            sessionId.trim(), 
            request.getNumberOfRounds(),
            request.getRoundDuration(),
            request.getCategories()
        );

        // Return updated game state with server-generated letter
        GameStateDTO gameState = gameSessionMapper.toGameStateDTO(session);
        
        logger.info("Round started with config - sessionId: {}, letter: {}, duration: {}, rounds: {}", 
                   sessionId, session.getLetter(), session.getRoundDuration(), request.getNumberOfRounds());

        return ResponseEntity.ok(gameState);
    }

    /**
     * Submit player results for a round.
     * Server compares and validates all results.
     * 
     * @param request the submit results request
     * @return ResponseEntity with submission confirmation
     */
    @PostMapping("/submit")
    public ResponseEntity<String> submitResults(@RequestBody SubmitResultsRequest request) {
        logger.info("Results submission request - sessionId: {}, player: {}, answers: {}", 
                   request.getSessionId(), request.getPlayerUsername(), request.getResults().size());

        // Validate request
        if (request == null || request.getSessionId() == null || 
            request.getSessionId().trim().isEmpty()) {
            throw new IllegalArgumentException("Session ID is required");
        }
        
        if (request.getPlayerUsername() == null || 
            request.getPlayerUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Player username is required");
        }
        
        if (request.getResults() == null || request.getResults().isEmpty()) {
            throw new IllegalArgumentException("Results cannot be empty");
        }

        // Submit results - server validates and processes
        boolean success = gameSessionService.submitResults(
                request.getSessionId().trim(),
                request.getPlayerUsername().trim(),
                request.getResults()
        );

        if (success) {
            logger.info("Results submission successful - sessionId: {}, player: {}", 
                       request.getSessionId(), request.getPlayerUsername());
            return ResponseEntity.ok("Results submitted successfully");
        } else {
            logger.warn("Results submission failed - sessionId: {}, player: {}", 
                       request.getSessionId(), request.getPlayerUsername());
            return ResponseEntity.badRequest().body("Failed to submit results");
        }
    }

    /**
     * Get final results for a completed round.
     * Server compares all player results and returns final scoring.
     * 
     * @param sessionId the session ID
     * @return ResponseEntity containing the round results
     */
    @GetMapping("/{sessionId}/results")
    public ResponseEntity<RoundResultsDTO> getRoundResults(@PathVariable String sessionId) {
        logger.info("Round results request - sessionId: {}", sessionId);

        // Validate sessionId
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Session ID is required");
        }

        // Find the session
        Optional<GameSessionEntity> sessionOpt = gameSessionService.findBySessionId(sessionId.trim());
        if (sessionOpt.isEmpty()) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        // Process results if needed
        GameSessionEntity session = sessionOpt.get();
        if (session.getStatus().name().equals("RESULTS_PENDING")) {
            gameSessionService.processResults(sessionId.trim());
            // Refresh session data
            session = gameSessionService.findBySessionId(sessionId.trim()).orElse(session);
        }

        // Return round results with server comparison
        RoundResultsDTO results = gameSessionMapper.toRoundResultsDTO(session);
        
        logger.info("Round results returned - sessionId: {}, winner: {}", 
                   sessionId, results.getWinner());

        return ResponseEntity.ok(results);
    }
}
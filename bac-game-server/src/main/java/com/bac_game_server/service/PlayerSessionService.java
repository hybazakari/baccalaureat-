package com.bac_game_server.service;

import com.bac_game_server.entity.GameSessionEntity;
import com.bac_game_server.entity.PlayerEntity;
import com.bac_game_server.entity.PlayerSessionEntity;
import com.bac_game_server.repository.PlayerSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service class responsible for player session management in word game sessions.
 * Handles player participation, result submission, and scoring.
 */
@Service
@Transactional
public class PlayerSessionService {

    private static final Logger logger = LoggerFactory.getLogger(PlayerSessionService.class);
    
    private final PlayerSessionRepository playerSessionRepository;

    @Autowired
    public PlayerSessionService(PlayerSessionRepository playerSessionRepository) {
        this.playerSessionRepository = playerSessionRepository;
    }

    /**
     * Create a new player session for a player joining a game session.
     * 
     * @param player the player joining the session
     * @param gameSession the session being joined
     * @param isHost whether this player is the host
     * @return the created player session
     * @throws IllegalArgumentException if player is already in the session
     */
    public PlayerSessionEntity createSession(PlayerEntity player, GameSessionEntity gameSession, boolean isHost) {
        logger.info("Creating player session - player: {}, sessionId: {}, isHost: {}", 
                   player.getUsername(), gameSession.getSessionId(), isHost);

        if (player == null || gameSession == null) {
            throw new IllegalArgumentException("Player and GameSession cannot be null");
        }

        if (isPlayerInSession(player, gameSession)) {
            throw new IllegalArgumentException("Player " + player.getUsername() + 
                    " is already in session " + gameSession.getSessionId());
        }

        PlayerSessionEntity session = new PlayerSessionEntity(player, gameSession, isHost);
        session = playerSessionRepository.save(session);
        
        logger.info("Player session created - sessionId: {}, player: {}, playerId: {}", 
                   gameSession.getSessionId(), player.getUsername(), player.getId());
        return session;
    }

    /**
     * Check if a player is already in a specific game session.
     * 
     * @param player the player to check
     * @param gameSession the session to check
     * @return true if the player is in the session, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isPlayerInSession(PlayerEntity player, GameSessionEntity gameSession) {
        if (player == null || gameSession == null) {
            return false;
        }
        return playerSessionRepository.existsByPlayerAndGameSession(player, gameSession);
    }

    /**
     * Find a specific session for a player in a game session.
     * 
     * @param player the player
     * @param gameSession the session
     * @return Optional containing the session if found, empty otherwise
     */
    @Transactional(readOnly = true)
    public Optional<PlayerSessionEntity> findSession(PlayerEntity player, GameSessionEntity gameSession) {
        if (player == null || gameSession == null) {
            return Optional.empty();
        }
        return playerSessionRepository.findByPlayerAndGameSession(player, gameSession);
    }

    /**
     * Get all sessions for players in a specific game session.
     * 
     * @param gameSession the session to get players for
     * @return list of player sessions in the session
     */
    @Transactional(readOnly = true)
    public List<PlayerSessionEntity> getSessionsInSession(GameSessionEntity gameSession) {
        if (gameSession == null) {
            return List.of();
        }
        return playerSessionRepository.findByGameSession(gameSession);
    }

    /**
     * Submit results for a player in a session.
     * Server validates and stores the results.
     * 
     * @param player the player submitting results
     * @param gameSession the game session
     * @param results the player's answers (category -> answer)
     * @return true if submission was successful
     */
    public boolean submitResults(PlayerEntity player, GameSessionEntity gameSession, Map<String, String> results) {
        logger.info("Submitting results - sessionId: {}, player: {}, playerId: {}", 
                   gameSession.getSessionId(), player.getUsername(), player.getId());

        PlayerSessionEntity session = findSession(player, gameSession)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Player " + player.getUsername() + " is not in session " + gameSession.getSessionId()));

        if (session.isHasSubmitted()) {
            logger.warn("Player already submitted results - sessionId: {}, player: {}", 
                       gameSession.getSessionId(), player.getUsername());
            return false;
        }

        // Submit results
        session.submitResults(results);
        playerSessionRepository.save(session);
        
        logger.info("Results submitted successfully - sessionId: {}, player: {}, playerId: {}", 
                   gameSession.getSessionId(), player.getUsername(), player.getId());
        return true;
    }

    /**
     * Process all player results for a session and calculate scores.
     * Server compares all results and applies scoring logic.
     * 
     * @param gameSession the game session
     * @param gameLetter the letter for this round
     */
    public void processAllResults(GameSessionEntity gameSession, String gameLetter) {
        logger.info("Processing all results - sessionId: {}, letter: {}", 
                   gameSession.getSessionId(), gameLetter);

        List<PlayerSessionEntity> sessions = playerSessionRepository.findByGameSession(gameSession);
        
        for (PlayerSessionEntity session : sessions) {
            if (session.isHasSubmitted()) {
                // Calculate scores for this player
                session.calculateScores(gameLetter);
                playerSessionRepository.save(session);
                
                logger.info("Scores calculated - sessionId: {}, player: {}, totalScore: {}", 
                           gameSession.getSessionId(), session.getPlayer().getUsername(), 
                           session.getTotalScore());
            }
        }
        
        logger.info("All results processed - sessionId: {}", gameSession.getSessionId());
    }

    /**
     * Get sessions in a session ordered by score (highest first) for leaderboard.
     * 
     * @param gameSession the session to get leaderboard for
     * @return list of sessions ordered by score descending
     */
    @Transactional(readOnly = true)
    public List<PlayerSessionEntity> getLeaderboard(GameSessionEntity gameSession) {
        if (gameSession == null) {
            return List.of();
        }
        return playerSessionRepository.findByGameSessionOrderByScoreDesc(gameSession);
    }

    /**
     * Count the number of players in a specific game session.
     * 
     * @param gameSession the session to count players for
     * @return the number of players in the session
     */
    @Transactional(readOnly = true)
    public long countPlayersInSession(GameSessionEntity gameSession) {
        if (gameSession == null) {
            return 0;
        }
        return playerSessionRepository.countByGameSession(gameSession);
    }

    /**
     * Count the number of players who have submitted results in a session.
     * 
     * @param gameSession the session to count for
     * @return the number of submitted results
     */
    @Transactional(readOnly = true)
    public long countSubmittedResultsInSession(GameSessionEntity gameSession) {
        if (gameSession == null) {
            return 0;
        }
        return playerSessionRepository.countSubmittedResultsInSession(gameSession);
    }

    /**
     * Find the top scorer in a specific game session.
     * 
     * @param gameSession the session to find top scorer for
     * @return Optional containing the top scorer session, empty if no players in session
     */
    @Transactional(readOnly = true)
    public Optional<PlayerSessionEntity> findTopScorer(GameSessionEntity gameSession) {
        if (gameSession == null) {
            return Optional.empty();
        }
        return playerSessionRepository.findTopScorerInSession(gameSession);
    }

    /**
     * Get all players who have submitted results in a session.
     * 
     * @param gameSession the game session
     * @return list of sessions with submitted results
     */
    @Transactional(readOnly = true)
    public List<PlayerSessionEntity> getSubmittedResults(GameSessionEntity gameSession) {
        if (gameSession == null) {
            return List.of();
        }
        return playerSessionRepository.findSubmittedResultsInSession(gameSession);
    }

    /**
     * Get all players who haven't submitted results in a session.
     * 
     * @param gameSession the game session
     * @return list of sessions with pending results
     */
    @Transactional(readOnly = true)
    public List<PlayerSessionEntity> getPendingResults(GameSessionEntity gameSession) {
        if (gameSession == null) {
            return List.of();
        }
        return playerSessionRepository.findPendingResultsInSession(gameSession);
    }

    /**
     * Remove a player from a game session by deleting their session.
     * 
     * @param player the player to remove
     * @param gameSession the session to remove the player from
     * @throws IllegalArgumentException if the player is not in the session
     */
    public void removePlayerFromSession(PlayerEntity player, GameSessionEntity gameSession) {
        PlayerSessionEntity session = findSession(player, gameSession)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Player " + player.getUsername() + " is not in session " + gameSession.getSessionId()));

        playerSessionRepository.delete(session);
        
        logger.info("Player removed from session - sessionId: {}, player: {}, playerId: {}", 
                   gameSession.getSessionId(), player.getUsername(), player.getId());
    }
}
package com.bac_game_server.repository;

import com.bac_game_server.entity.GameSessionEntity;
import com.bac_game_server.entity.PlayerEntity;
import com.bac_game_server.entity.PlayerSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for PlayerSessionEntity.
 * Provides basic CRUD operations and custom query methods for managing player sessions.
 */
@Repository
public interface PlayerSessionRepository extends JpaRepository<PlayerSessionEntity, Long> {

    /**
     * Find all sessions for a specific player.
     * 
     * @param player the player entity
     * @return list of sessions for the specified player
     */
    List<PlayerSessionEntity> findByPlayer(PlayerEntity player);

    /**
     * Find all sessions in a specific game session.
     * 
     * @param gameSession the game session entity
     * @return list of sessions in the specified game session
     */
    List<PlayerSessionEntity> findByGameSession(GameSessionEntity gameSession);

    /**
     * Find a specific session for a player in a game session.
     * 
     * @param player the player entity
     * @param gameSession the game session entity
     * @return Optional containing the session if found, empty otherwise
     */
    Optional<PlayerSessionEntity> findByPlayerAndGameSession(PlayerEntity player, GameSessionEntity gameSession);

    /**
     * Check if a player is already in a specific game session.
     * 
     * @param player the player entity
     * @param gameSession the game session entity
     * @return true if the player is already in the session, false otherwise
     */
    boolean existsByPlayerAndGameSession(PlayerEntity player, GameSessionEntity gameSession);

    /**
     * Count the number of players in a specific game session.
     * 
     * @param gameSession the game session entity
     * @return the number of players in the session
     */
    long countByGameSession(GameSessionEntity gameSession);

    /**
     * Find sessions ordered by total score in descending order for a specific game session.
     * This is useful for leaderboards.
     * 
     * @param gameSession the game session entity
     * @return list of sessions ordered by score (highest first)
     */
    @Query("SELECT ps FROM PlayerSessionEntity ps WHERE ps.gameSession = :gameSession ORDER BY ps.totalScore DESC")
    List<PlayerSessionEntity> findByGameSessionOrderByScoreDesc(GameSessionEntity gameSession);

    /**
     * Find the top scorer in a specific game session.
     * 
     * @param gameSession the game session entity
     * @return Optional containing the top scorer session, empty if no sessions exist
     */
    @Query("SELECT ps FROM PlayerSessionEntity ps WHERE ps.gameSession = :gameSession ORDER BY ps.totalScore DESC LIMIT 1")
    Optional<PlayerSessionEntity> findTopScorerInSession(GameSessionEntity gameSession);

    /**
     * Find all players who have submitted results in a specific session.
     * 
     * @param gameSession the game session entity
     * @return list of sessions where players have submitted
     */
    @Query("SELECT ps FROM PlayerSessionEntity ps WHERE ps.gameSession = :gameSession AND ps.hasSubmitted = true")
    List<PlayerSessionEntity> findSubmittedResultsInSession(GameSessionEntity gameSession);

    /**
     * Find all players who haven't submitted results in a specific session.
     * 
     * @param gameSession the game session entity
     * @return list of sessions where players haven't submitted
     */
    @Query("SELECT ps FROM PlayerSessionEntity ps WHERE ps.gameSession = :gameSession AND ps.hasSubmitted = false")
    List<PlayerSessionEntity> findPendingResultsInSession(GameSessionEntity gameSession);

    /**
     * Count how many players have submitted results in a specific session.
     * 
     * @param gameSession the game session entity
     * @return count of submitted results
     */
    @Query("SELECT COUNT(ps) FROM PlayerSessionEntity ps WHERE ps.gameSession = :gameSession AND ps.hasSubmitted = true")
    long countSubmittedResultsInSession(GameSessionEntity gameSession);

    /**
     * Find the host player session in a specific game session.
     * 
     * @param gameSession the game session entity
     * @return Optional containing the host session, empty if not found
     */
    @Query("SELECT ps FROM PlayerSessionEntity ps WHERE ps.gameSession = :gameSession AND ps.isHost = true")
    Optional<PlayerSessionEntity> findHostInSession(GameSessionEntity gameSession);

    /**
     * Delete all sessions for a specific game session.
     * This can be used when cleaning up finished games.
     * 
     * @param gameSession the game session entity
     */
    void deleteByGameSession(GameSessionEntity gameSession);
}
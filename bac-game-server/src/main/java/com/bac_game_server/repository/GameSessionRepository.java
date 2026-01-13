package com.bac_game_server.repository;

import com.bac_game_server.entity.GameSessionEntity;
import com.bac_game_server.entity.GameSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for GameSessionEntity.
 * Provides basic CRUD operations and custom query methods for game session management.
 */
@Repository
public interface GameSessionRepository extends JpaRepository<GameSessionEntity, Long> {

    /**
     * Find a game session by its unique sessionId.
     * 
     * @param sessionId the session ID to search for
     * @return Optional containing the game session if found, empty otherwise
     */
    Optional<GameSessionEntity> findBySessionId(String sessionId);

    /**
     * Check if a game session exists with the given sessionId.
     * 
     * @param sessionId the session ID to check
     * @return true if a session exists with this ID, false otherwise
     */
    boolean existsBySessionId(String sessionId);

    /**
     * Find all game sessions with a specific status.
     * 
     * @param status the status to filter by
     * @return list of game sessions with the specified status
     */
    List<GameSessionEntity> findByStatus(GameSessionStatus status);

    /**
     * Find all sessions created by a specific host.
     * 
     * @param hostUsername the host's username
     * @return list of sessions created by the host
     */
    List<GameSessionEntity> findByHostUsername(String hostUsername);

    /**
     * Find all waiting sessions (sessions that can still accept players).
     * 
     * @return list of waiting sessions ordered by creation time
     */
    @Query("SELECT s FROM GameSessionEntity s WHERE s.status = 'WAITING' ORDER BY s.createdAt ASC")
    List<GameSessionEntity> findWaitingSessionsOrderByCreatedAt();

    /**
     * Find sessions that are currently in progress.
     * 
     * @return list of active sessions
     */
    @Query("SELECT s FROM GameSessionEntity s WHERE s.status = 'IN_PROGRESS'")
    List<GameSessionEntity> findActiveSessions();

    /**
     * Find sessions where the round has expired but status hasn't been updated.
     * 
     * @param currentTime the current time
     * @return list of expired sessions
     */
    @Query("SELECT s FROM GameSessionEntity s WHERE s.status = 'IN_PROGRESS' AND s.roundEndTime < :currentTime")
    List<GameSessionEntity> findExpiredSessions(LocalDateTime currentTime);

    /**
     * Find stale waiting sessions (sessions that have been waiting too long).
     * 
     * @param threshold the time threshold
     * @return list of stale sessions
     */
    @Query("SELECT s FROM GameSessionEntity s WHERE s.status = 'WAITING' AND s.createdAt < :threshold")
    List<GameSessionEntity> findStaleWaitingSessions(LocalDateTime threshold);
}
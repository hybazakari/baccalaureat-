package com.bac_game_server.entity;

/**
 * Enumeration representing the possible states of a game session.
 */
public enum GameSessionStatus {
    
    /**
     * Session is created and waiting for players to join
     */
    WAITING,
    
    /**
     * Round is currently in progress
     */
    IN_PROGRESS,
    
    /**
     * Round has ended, results being processed
     */
    RESULTS_PENDING,
    
    /**
     * Session has ended
     */
    FINISHED
}
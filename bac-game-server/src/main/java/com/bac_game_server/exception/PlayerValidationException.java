package com.bac_game_server.exception;

/**
 * Exception thrown when player-related validation fails.
 * This includes issues like invalid usernames, duplicate players, etc.
 */
public class PlayerValidationException extends RuntimeException {

    private final String username;

    public PlayerValidationException(String username, String message) {
        super(message);
        this.username = username;
    }

    public PlayerValidationException(String username, String message, Throwable cause) {
        super(message, cause);
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
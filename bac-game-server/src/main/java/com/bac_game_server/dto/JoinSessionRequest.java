package com.bac_game_server.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data Transfer Object for joining an existing multiplayer session.
 * SessionId is the only way to join a game.
 */
public class JoinSessionRequest {
    
    @JsonProperty("sessionId")
    private String sessionId;
    
    @JsonProperty("playerUsername")
    private String playerUsername;

    // Default constructor for Jackson
    public JoinSessionRequest() {
    }

    public JoinSessionRequest(String sessionId, String playerUsername) {
        this.sessionId = sessionId;
        this.playerUsername = playerUsername;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getPlayerUsername() {
        return playerUsername;
    }

    public void setPlayerUsername(String playerUsername) {
        this.playerUsername = playerUsername;
    }

    @Override
    public String toString() {
        return "JoinSessionRequest{" +
                "sessionId='" + sessionId + '\'' +
                ", playerUsername='" + playerUsername + '\'' +
                '}';
    }
}
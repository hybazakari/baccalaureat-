package com.bac_game_server.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data Transfer Object for session creation response.
 * Must contain sessionId as the single source of truth.
 */
public class CreateSessionResponse {
    
    @JsonProperty("sessionId")
    private String sessionId;
    
    @JsonProperty("hostUsername")
    private String hostUsername;
    
    @JsonProperty("message")
    private String message;

    // Default constructor for Jackson
    public CreateSessionResponse() {
    }

    public CreateSessionResponse(String sessionId, String hostUsername) {
        this.sessionId = sessionId;
        this.hostUsername = hostUsername;
        this.message = "Session created successfully";
    }

    public CreateSessionResponse(String sessionId, String hostUsername, String message) {
        this.sessionId = sessionId;
        this.hostUsername = hostUsername;
        this.message = message;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getHostUsername() {
        return hostUsername;
    }

    public void setHostUsername(String hostUsername) {
        this.hostUsername = hostUsername;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "CreateSessionResponse{" +
                "sessionId='" + sessionId + '\'' +
                ", hostUsername='" + hostUsername + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
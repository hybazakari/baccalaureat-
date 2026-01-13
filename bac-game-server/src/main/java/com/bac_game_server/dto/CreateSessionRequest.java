package com.bac_game_server.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Data Transfer Object for creating a new multiplayer session.
 * The server is the single source of truth for session creation.
 */
public class CreateSessionRequest {
    
    @JsonProperty("hostUsername")
    private String hostUsername;
    
    @JsonProperty("categories")
    private List<String> categories;
    
    @JsonProperty("roundDuration")
    private int roundDuration; // in seconds

    // Default constructor for Jackson
    public CreateSessionRequest() {
    }

    public CreateSessionRequest(String hostUsername, List<String> categories, int roundDuration) {
        this.hostUsername = hostUsername;
        this.categories = categories;
        this.roundDuration = roundDuration;
    }

    public String getHostUsername() {
        return hostUsername;
    }

    public void setHostUsername(String hostUsername) {
        this.hostUsername = hostUsername;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public int getRoundDuration() {
        return roundDuration;
    }

    public void setRoundDuration(int roundDuration) {
        this.roundDuration = roundDuration;
    }

    @Override
    public String toString() {
        return "CreateSessionRequest{" +
                "hostUsername='" + hostUsername + '\'' +
                ", categories=" + categories +
                ", roundDuration=" + roundDuration +
                '}';
    }
}
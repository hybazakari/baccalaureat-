package com.bac_game_server.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Data Transfer Object for starting a game with configuration.
 * Contains all game configuration parameters from the host.
 */
public class StartGameRequest {
    
    @JsonProperty("numberOfRounds")
    private int numberOfRounds;
    
    @JsonProperty("roundDuration")
    private int roundDuration; // in seconds
    
    @JsonProperty("categories")
    private List<String> categories;

    // Default constructor for Jackson
    public StartGameRequest() {
    }

    public StartGameRequest(int numberOfRounds, int roundDuration, List<String> categories) {
        this.numberOfRounds = numberOfRounds;
        this.roundDuration = roundDuration;
        this.categories = categories;
    }

    public int getNumberOfRounds() {
        return numberOfRounds;
    }

    public void setNumberOfRounds(int numberOfRounds) {
        this.numberOfRounds = numberOfRounds;
    }

    public int getRoundDuration() {
        return roundDuration;
    }

    public void setRoundDuration(int roundDuration) {
        this.roundDuration = roundDuration;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    @Override
    public String toString() {
        return "StartGameRequest{" +
                "numberOfRounds=" + numberOfRounds +
                ", roundDuration=" + roundDuration +
                ", categories=" + categories +
                '}';
    }
}
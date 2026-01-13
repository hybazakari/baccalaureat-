package com.bac_game_server.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Data Transfer Object for submitting player results at end of round.
 * Server will compare and validate results.
 */
public class SubmitResultsRequest {
    
    @JsonProperty("sessionId")
    private String sessionId;
    
    @JsonProperty("playerUsername")
    private String playerUsername;
    
    @JsonProperty("results")
    private Map<String, String> results; // category -> player's answer
    
    @JsonProperty("submissionTime")
    private long submissionTime; // timestamp of submission

    // Default constructor for Jackson
    public SubmitResultsRequest() {
    }

    public SubmitResultsRequest(String sessionId, String playerUsername, 
                               Map<String, String> results, long submissionTime) {
        this.sessionId = sessionId;
        this.playerUsername = playerUsername;
        this.results = results;
        this.submissionTime = submissionTime;
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

    public Map<String, String> getResults() {
        return results;
    }

    public void setResults(Map<String, String> results) {
        this.results = results;
    }

    public long getSubmissionTime() {
        return submissionTime;
    }

    public void setSubmissionTime(long submissionTime) {
        this.submissionTime = submissionTime;
    }

    @Override
    public String toString() {
        return "SubmitResultsRequest{" +
                "sessionId='" + sessionId + '\'' +
                ", playerUsername='" + playerUsername + '\'' +
                ", results=" + results +
                ", submissionTime=" + submissionTime +
                '}';
    }
}
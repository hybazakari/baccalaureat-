package com.bac_game_server.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO for submitting player answers at round end.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubmitAnswersRequest {
    private String sessionId;
    private String playerName;
    private java.util.Map<String, String> answers;
    private int score;

    public SubmitAnswersRequest() {}

    public SubmitAnswersRequest(String sessionId, String playerName, 
                               java.util.Map<String, String> answers, int score) {
        this.sessionId = sessionId;
        this.playerName = playerName;
        this.answers = answers;
        this.score = score;
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public java.util.Map<String, String> getAnswers() { return answers; }
    public void setAnswers(java.util.Map<String, String> answers) { this.answers = answers; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
}
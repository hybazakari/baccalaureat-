package com.bac_game_server.dto;

import java.util.List;
import java.util.Map;

/**
 * DTO for player score information in multiplayer games.
 */
public class PlayerScoreDTO {
    private String playerName;
    private int totalScore;
    private int roundScore;
    private Map<String, String> answers;
    private int rank; // Position in leaderboard

    public PlayerScoreDTO() {}

    public PlayerScoreDTO(String playerName, int totalScore, int roundScore, 
                          Map<String, String> answers, int rank) {
        this.playerName = playerName;
        this.totalScore = totalScore;
        this.roundScore = roundScore;
        this.answers = answers;
        this.rank = rank;
    }

    // Getters and setters
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public int getTotalScore() { return totalScore; }
    public void setTotalScore(int totalScore) { this.totalScore = totalScore; }

    public int getRoundScore() { return roundScore; }
    public void setRoundScore(int roundScore) { this.roundScore = roundScore; }

    public Map<String, String> getAnswers() { return answers; }
    public void setAnswers(Map<String, String> answers) { this.answers = answers; }

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }
}
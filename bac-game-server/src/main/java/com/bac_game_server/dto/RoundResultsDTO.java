package com.bac_game_server.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object for round results after server comparison.
 * Server compares all player results and returns final scoring.
 */
public class RoundResultsDTO {
    
    @JsonProperty("sessionId")
    private String sessionId;
    
    @JsonProperty("letter")
    private String letter;
    
    @JsonProperty("playerResults")
    private List<PlayerResultDTO> playerResults;
    
    @JsonProperty("roundComplete")
    private boolean roundComplete;
    
    @JsonProperty("winner")
    private String winner; // username of winner, null if tie

    // Default constructor for Jackson
    public RoundResultsDTO() {
    }

    public RoundResultsDTO(String sessionId, String letter, List<PlayerResultDTO> playerResults, 
                          boolean roundComplete, String winner) {
        this.sessionId = sessionId;
        this.letter = letter;
        this.playerResults = playerResults;
        this.roundComplete = roundComplete;
        this.winner = winner;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getLetter() {
        return letter;
    }

    public void setLetter(String letter) {
        this.letter = letter;
    }

    public List<PlayerResultDTO> getPlayerResults() {
        return playerResults;
    }

    public void setPlayerResults(List<PlayerResultDTO> playerResults) {
        this.playerResults = playerResults;
    }

    public boolean isRoundComplete() {
        return roundComplete;
    }

    public void setRoundComplete(boolean roundComplete) {
        this.roundComplete = roundComplete;
    }

    public String getWinner() {
        return winner;
    }

    public void setWinner(String winner) {
        this.winner = winner;
    }

    @Override
    public String toString() {
        return "RoundResultsDTO{" +
                "sessionId='" + sessionId + '\'' +
                ", letter='" + letter + '\'' +
                ", playerResults=" + playerResults +
                ", roundComplete=" + roundComplete +
                ", winner='" + winner + '\'' +
                '}';
    }

    /**
     * Nested DTO representing individual player results.
     */
    public static class PlayerResultDTO {
        
        @JsonProperty("username")
        private String username;
        
        @JsonProperty("answers")
        private Map<String, String> answers; // category -> answer
        
        @JsonProperty("scores")
        private Map<String, Integer> scores; // category -> score
        
        @JsonProperty("totalScore")
        private int totalScore;
        
        @JsonProperty("validAnswers")
        private int validAnswers;
        
        @JsonProperty("invalidAnswers")
        private int invalidAnswers;

        // Default constructor for Jackson
        public PlayerResultDTO() {
        }

        public PlayerResultDTO(String username, Map<String, String> answers, Map<String, Integer> scores, 
                              int totalScore, int validAnswers, int invalidAnswers) {
            this.username = username;
            this.answers = answers;
            this.scores = scores;
            this.totalScore = totalScore;
            this.validAnswers = validAnswers;
            this.invalidAnswers = invalidAnswers;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public Map<String, String> getAnswers() {
            return answers;
        }

        public void setAnswers(Map<String, String> answers) {
            this.answers = answers;
        }

        public Map<String, Integer> getScores() {
            return scores;
        }

        public void setScores(Map<String, Integer> scores) {
            this.scores = scores;
        }

        public int getTotalScore() {
            return totalScore;
        }

        public void setTotalScore(int totalScore) {
            this.totalScore = totalScore;
        }

        public int getValidAnswers() {
            return validAnswers;
        }

        public void setValidAnswers(int validAnswers) {
            this.validAnswers = validAnswers;
        }

        public int getInvalidAnswers() {
            return invalidAnswers;
        }

        public void setInvalidAnswers(int invalidAnswers) {
            this.invalidAnswers = invalidAnswers;
        }

        @Override
        public String toString() {
            return "PlayerResultDTO{" +
                    "username='" + username + '\'' +
                    ", answers=" + answers +
                    ", scores=" + scores +
                    ", totalScore=" + totalScore +
                    ", validAnswers=" + validAnswers +
                    ", invalidAnswers=" + invalidAnswers +
                    '}';
        }
    }
}
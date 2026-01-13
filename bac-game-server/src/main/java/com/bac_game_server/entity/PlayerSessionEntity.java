package com.bac_game_server.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * JPA Entity representing a player's session within a multiplayer word game.
 * Tracks player participation, submission status, and results.
 */
@Entity
@Table(name = "player_sessions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"player_id", "session_id"}))
public class PlayerSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private PlayerEntity player;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private GameSessionEntity gameSession;

    @Column(name = "is_host", nullable = false)
    private boolean isHost;

    @Column(name = "has_submitted", nullable = false)
    private boolean hasSubmitted;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @ElementCollection
    @CollectionTable(name = "player_results", joinColumns = @JoinColumn(name = "player_session_id"))
    @MapKeyColumn(name = "category")
    @Column(name = "answer")
    private Map<String, String> results = new HashMap<>();

    @ElementCollection
    @CollectionTable(name = "player_scores", joinColumns = @JoinColumn(name = "player_session_id"))
    @MapKeyColumn(name = "category")
    @Column(name = "score")
    private Map<String, Integer> scores = new HashMap<>();

    @Column(name = "total_score", nullable = false)
    private int totalScore;

    // Default constructor for JPA
    protected PlayerSessionEntity() {
    }

    public PlayerSessionEntity(PlayerEntity player, GameSessionEntity gameSession, boolean isHost) {
        this.player = player;
        this.gameSession = gameSession;
        this.isHost = isHost;
        this.hasSubmitted = false;
        this.totalScore = 0;
        this.joinedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
    }

    // Business methods
    public void submitResults(Map<String, String> playerResults) {
        this.results.clear();
        this.results.putAll(playerResults);
        this.hasSubmitted = true;
        this.submittedAt = LocalDateTime.now();
    }

    public void calculateScores(String gameLetter) {
        this.scores.clear();
        this.totalScore = 0;
        
        for (Map.Entry<String, String> entry : results.entrySet()) {
            String category = entry.getKey();
            String answer = entry.getValue();
            int score = calculateAnswerScore(answer, gameLetter);
            scores.put(category, score);
            totalScore += score;
        }
    }

    private int calculateAnswerScore(String answer, String expectedLetter) {
        if (answer == null || answer.trim().isEmpty()) {
            return 0;
        }
        
        String trimmedAnswer = answer.trim().toUpperCase();
        String expectedUppercase = expectedLetter.toUpperCase();
        
        // Basic scoring: 10 points if starts with correct letter, 0 otherwise
        if (trimmedAnswer.startsWith(expectedUppercase)) {
            return 10;
        }
        return 0;
    }

    public void resetForNewRound() {
        this.hasSubmitted = false;
        this.submittedAt = null;
        this.results.clear();
        this.scores.clear();
        this.totalScore = 0;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PlayerEntity getPlayer() {
        return player;
    }

    public void setPlayer(PlayerEntity player) {
        this.player = player;
    }

    public GameSessionEntity getGameSession() {
        return gameSession;
    }

    public void setGameSession(GameSessionEntity gameSession) {
        this.gameSession = gameSession;
    }

    public boolean isHost() {
        return isHost;
    }

    public void setHost(boolean host) {
        isHost = host;
    }

    public boolean isHasSubmitted() {
        return hasSubmitted;
    }

    public void setHasSubmitted(boolean hasSubmitted) {
        this.hasSubmitted = hasSubmitted;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Map<String, String> getResults() {
        return results;
    }

    public void setResults(Map<String, String> results) {
        this.results = results;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerSessionEntity that = (PlayerSessionEntity) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(player, that.player) &&
               Objects.equals(gameSession, that.gameSession);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, player, gameSession);
    }

    @Override
    public String toString() {
        return "PlayerSessionEntity{" +
                "id=" + id +
                ", player=" + (player != null ? player.getUsername() : "null") +
                ", gameSession=" + (gameSession != null ? gameSession.getSessionId() : "null") +
                ", isHost=" + isHost +
                ", hasSubmitted=" + hasSubmitted +
                ", totalScore=" + totalScore +
                ", joinedAt=" + joinedAt +
                '}';
    }
}
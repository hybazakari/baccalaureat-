package com.bac_game_server.mapper;

import com.bac_game_server.dto.GameStateDTO;
import com.bac_game_server.dto.RoundResultsDTO;
import com.bac_game_server.entity.GameSessionEntity;
import com.bac_game_server.entity.PlayerSessionEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class for mapping between game session entities and DTOs.
 * Ensures clean separation between internal data models and external API representations.
 */
@Component
public class GameSessionMapper {

    /**
     * Convert a GameSessionEntity to a GameStateDTO.
     * 
     * @param entity the game session entity to convert
     * @return the converted DTO
     */
    public GameStateDTO toGameStateDTO(GameSessionEntity entity) {
        if (entity == null) {
            return null;
        }

        List<GameStateDTO.PlayerStateDTO> players = entity.getPlayerSessions()
                .stream()
                .map(this::toPlayerStateDTO)
                .collect(Collectors.toList());

        GameStateDTO gameState = new GameStateDTO(
                entity.getSessionId(),
                entity.getLetter(),
                entity.getCategories(),
                entity.getRoundDuration(),
                players,
                entity.getStatus().name()
        );
        
        gameState.setRoundStartTime(entity.getRoundStartTime());
        gameState.setRoundEndTime(entity.getRoundEndTime());
        
        return gameState;
    }

    /**
     * Convert a GameSessionEntity to a RoundResultsDTO after results processing.
     * 
     * @param entity the game session entity to convert
     * @return the converted results DTO
     */
    public RoundResultsDTO toRoundResultsDTO(GameSessionEntity entity) {
        if (entity == null) {
            return null;
        }

        List<RoundResultsDTO.PlayerResultDTO> playerResults = entity.getPlayerSessions()
                .stream()
                .filter(PlayerSessionEntity::isHasSubmitted)
                .map(this::toPlayerResultDTO)
                .collect(Collectors.toList());

        // Determine winner (highest total score)
        String winner = playerResults.stream()
                .max((p1, p2) -> Integer.compare(p1.getTotalScore(), p2.getTotalScore()))
                .map(RoundResultsDTO.PlayerResultDTO::getUsername)
                .orElse(null);

        // Check if all players have submitted (round complete)
        long totalPlayers = entity.getPlayerSessions().size();
        long submittedPlayers = playerResults.size();
        boolean roundComplete = submittedPlayers >= totalPlayers && 
                               !entity.getStatus().name().equals("IN_PROGRESS");

        return new RoundResultsDTO(
                entity.getSessionId(),
                entity.getLetter(),
                playerResults,
                roundComplete,
                winner
        );
    }

    /**
     * Convert a PlayerSessionEntity to a PlayerStateDTO.
     * 
     * @param session the player session entity to convert
     * @return the converted player state DTO
     */
    private GameStateDTO.PlayerStateDTO toPlayerStateDTO(PlayerSessionEntity session) {
        if (session == null || session.getPlayer() == null) {
            return null;
        }

        return new GameStateDTO.PlayerStateDTO(
                session.getPlayer().getUsername(),
                session.isHost(),
                session.isHasSubmitted(),
                session.getJoinedAt()
        );
    }

    /**
     * Convert a PlayerSessionEntity to a PlayerResultDTO.
     * 
     * @param session the player session entity to convert
     * @return the converted player result DTO
     */
    private RoundResultsDTO.PlayerResultDTO toPlayerResultDTO(PlayerSessionEntity session) {
        if (session == null || session.getPlayer() == null) {
            return null;
        }

        // Count valid and invalid answers
        Map<String, String> answers = session.getResults();
        Map<String, Integer> scores = session.getScores();
        
        int validAnswers = 0;
        int invalidAnswers = 0;
        
        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            if (entry.getValue() > 0) {
                validAnswers++;
            } else {
                String answer = answers.get(entry.getKey());
                if (answer != null && !answer.trim().isEmpty()) {
                    invalidAnswers++;
                }
            }
        }

        return new RoundResultsDTO.PlayerResultDTO(
                session.getPlayer().getUsername(),
                answers,
                scores,
                session.getTotalScore(),
                validAnswers,
                invalidAnswers
        );
    }
}
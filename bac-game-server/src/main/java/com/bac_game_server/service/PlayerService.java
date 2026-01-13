package com.bac_game_server.service;

import com.bac_game_server.entity.PlayerEntity;
import com.bac_game_server.exception.PlayerValidationException;
import com.bac_game_server.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service class responsible for player management operations.
 * Handles player creation, retrieval, and validation.
 */
@Service
@Transactional
public class PlayerService {

    private final PlayerRepository playerRepository;

    @Autowired
    public PlayerService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    /**
     * Create a new player with the given username.
     * 
     * @param username the desired username
     * @return the created player entity
     * @throws IllegalArgumentException if username is null, empty, or already exists
     */
    public PlayerEntity createPlayer(String username) {
        validateUsername(username);
        
        if (playerRepository.existsByUsername(username)) {
            throw new PlayerValidationException(username, "Username '" + username + "' is already taken");
        }

        PlayerEntity player = new PlayerEntity(username);
        return playerRepository.save(player);
    }

    /**
     * Find a player by their username.
     * 
     * @param username the username to search for
     * @return Optional containing the player if found, empty otherwise
     */
    @Transactional(readOnly = true)
    public Optional<PlayerEntity> findByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return Optional.empty();
        }
        return playerRepository.findByUsername(username.trim());
    }

    /**
     * Find a player by their ID.
     * 
     * @param id the player ID
     * @return Optional containing the player if found, empty otherwise
     */
    @Transactional(readOnly = true)
    public Optional<PlayerEntity> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return playerRepository.findById(id);
    }

    /**
     * Get or create a player with the given username.
     * If a player with the username already exists, returns that player.
     * Otherwise, creates a new player.
     * 
     * @param username the username
     * @return the existing or newly created player entity
     * @throws IllegalArgumentException if username is null or empty
     */
    public PlayerEntity getOrCreatePlayer(String username) {
        validateUsername(username);
        
        return findByUsername(username)
                .orElseGet(() -> createPlayer(username));
    }

    /**
     * Check if a username is available.
     * 
     * @param username the username to check
     * @return true if the username is available, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isUsernameAvailable(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        return !playerRepository.existsByUsername(username.trim());
    }

    /**
     * Validate a username according to business rules.
     * 
     * @param username the username to validate
     * @throws IllegalArgumentException if the username is invalid
     */
    private void validateUsername(String username) {
        if (username == null) {
            throw new PlayerValidationException(null, "Username cannot be null");
        }
        
        String trimmed = username.trim();
        if (trimmed.isEmpty()) {
            throw new PlayerValidationException(username, "Username cannot be empty");
        }
        
        if (trimmed.length() < 2) {
            throw new PlayerValidationException(username, "Username must be at least 2 characters long");
        }
        
        if (trimmed.length() > 50) {
            throw new PlayerValidationException(username, "Username cannot be longer than 50 characters");
        }
        
        // Check for valid characters (alphanumeric, underscore, hyphen)
        if (!trimmed.matches("^[a-zA-Z0-9_-]+$")) {
            throw new PlayerValidationException(username, "Username can only contain letters, numbers, underscores, and hyphens");
        }
    }
}
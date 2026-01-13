package com.bac_game_server.repository;

import com.bac_game_server.entity.PlayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for PlayerEntity.
 * Provides basic CRUD operations and custom query methods.
 */
@Repository
public interface PlayerRepository extends JpaRepository<PlayerEntity, Long> {

    /**
     * Find a player by their username.
     * 
     * @param username the username to search for
     * @return Optional containing the player if found, empty otherwise
     */
    Optional<PlayerEntity> findByUsername(String username);

    /**
     * Check if a player exists with the given username.
     * 
     * @param username the username to check
     * @return true if a player exists with this username, false otherwise
     */
    boolean existsByUsername(String username);
}
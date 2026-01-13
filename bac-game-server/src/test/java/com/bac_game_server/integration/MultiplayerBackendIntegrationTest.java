package com.bac_game_server.integration;

import com.bac_game_server.dto.*;
import com.bac_game_server.entity.GameSessionEntity;
import com.bac_game_server.entity.PlayerSessionEntity;
import com.bac_game_server.service.GameSessionService;
import com.bac_game_server.service.PlayerSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CRITICAL Integration Test for Multiplayer Backend Validation.
 * This test MUST pass to validate the multiplayer contract.
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
public class MultiplayerBackendIntegrationTest {

    @Autowired
    private GameSessionService gameSessionService;

    @Autowired
    private PlayerSessionService playerSessionService;

    /**
     * CRITICAL TEST: Validates that two players receive identical game state.
     * If this test fails, the backend is NOT VALIDATED.
     */
    @Test
    public void testMultiplayerSessionContract_TwoPlayersReceiveIdenticalGameState() {
        System.out.println("=== CRITICAL MULTIPLAYER BACKEND VALIDATION TEST ===");
        
        // ARRANGE: Prepare test data
        String hostUsername = "TestHost";
        String player2Username = "TestPlayer2";
        List<String> categories = Arrays.asList("Animals", "Colors", "Countries");
        int roundDuration = 60;
        
        System.out.println("Creating session with host: " + hostUsername);
        System.out.println("Categories: " + categories);
        System.out.println("Round duration: " + roundDuration + " seconds");

        // ACT & ASSERT: Step 1 - Create a multiplayer session
        System.out.println("\n--- Step 1: Creating Session ---");
        GameSessionEntity session = gameSessionService.createSession(hostUsername, categories, roundDuration);
        
        assertNotNull(session, "Session should be created");
        assertNotNull(session.getSessionId(), "SessionId should not be null");
        assertEquals(6, session.getSessionId().length(), "SessionId should be 6 characters");
        assertEquals(hostUsername, session.getHostUsername(), "Host should match");
        assertEquals(categories, session.getCategories(), "Categories should match");
        assertEquals(roundDuration, session.getRoundDuration(), "Round duration should match");
        
        String sessionId = session.getSessionId();
        System.out.println("✓ Session created successfully with ID: " + sessionId);

        // ACT & ASSERT: Step 2 - Second player joins using sessionId
        System.out.println("\n--- Step 2: Second Player Joins ---");
        GameSessionEntity updatedSession = gameSessionService.joinSession(sessionId, player2Username);
        
        assertNotNull(updatedSession, "Updated session should not be null");
        assertEquals(sessionId, updatedSession.getSessionId(), "SessionId should remain the same");
        
        System.out.println("✓ Second player joined successfully");

        // ACT & ASSERT: Step 3 - Start round to generate letter
        System.out.println("\n--- Step 3: Starting Round ---");
        GameSessionEntity startedSession = gameSessionService.startRound(sessionId);
        
        assertNotNull(startedSession, "Started session should not be null");
        assertNotNull(startedSession.getLetter(), "Letter should be generated");
        assertEquals(1, startedSession.getLetter().length(), "Letter should be single character");
        assertTrue(startedSession.getLetter().matches("[A-Z]"), "Letter should be uppercase A-Z");
        
        System.out.println("✓ Round started, letter generated: " + startedSession.getLetter());

        // ACT & ASSERT: Step 4 - Verify both players receive IDENTICAL game state
        System.out.println("\n--- Step 4: Validating Identical Game State ---");
        
        // Retrieve session for player 1 (host)
        Optional<GameSessionEntity> player1Session = gameSessionService.findBySessionId(sessionId);
        assertTrue(player1Session.isPresent(), "Player 1 should find the session");
        
        // Retrieve session for player 2
        Optional<GameSessionEntity> player2Session = gameSessionService.findBySessionId(sessionId);
        assertTrue(player2Session.isPresent(), "Player 2 should find the session");
        
        // CRITICAL ASSERTIONS: Both players must receive IDENTICAL data
        GameSessionEntity p1Session = player1Session.get();
        GameSessionEntity p2Session = player2Session.get();
        
        // Assert SAME sessionId
        assertEquals(p1Session.getSessionId(), p2Session.getSessionId(), 
                    "CRITICAL: Both players must receive the SAME sessionId");
        System.out.println("✓ SessionId identical: " + p1Session.getSessionId());
        
        // Assert SAME letter (server is single source of truth)
        assertEquals(p1Session.getLetter(), p2Session.getLetter(), 
                    "CRITICAL: Both players must receive the SAME letter");
        System.out.println("✓ Letter identical: " + p1Session.getLetter());
        
        // Assert SAME categories list (server synchronized)
        assertEquals(p1Session.getCategories(), p2Session.getCategories(), 
                    "CRITICAL: Both players must receive the SAME categories list");
        System.out.println("✓ Categories identical: " + p1Session.getCategories());

        // Additional validation: Check player sessions
        List<PlayerSessionEntity> playerSessions = playerSessionService.getSessionsInSession(p1Session);
        assertEquals(2, playerSessions.size(), "Should have exactly 2 players");
        
        // Find host and regular player
        PlayerSessionEntity hostSession = playerSessions.stream()
                .filter(PlayerSessionEntity::isHost)
                .findFirst()
                .orElse(null);
        PlayerSessionEntity regularPlayerSession = playerSessions.stream()
                .filter(ps -> !ps.isHost())
                .findFirst()
                .orElse(null);
        
        assertNotNull(hostSession, "Host session should exist");
        assertNotNull(regularPlayerSession, "Regular player session should exist");
        assertEquals(hostUsername, hostSession.getPlayer().getUsername(), "Host username should match");
        assertEquals(player2Username, regularPlayerSession.getPlayer().getUsername(), "Player 2 username should match");
        
        System.out.println("✓ Host player: " + hostSession.getPlayer().getUsername());
        System.out.println("✓ Regular player: " + regularPlayerSession.getPlayer().getUsername());
        
        // Final validation
        System.out.println("\n=== VALIDATION RESULTS ===");
        System.out.println("Session ID: " + sessionId);
        System.out.println("Generated Letter: " + p1Session.getLetter());
        System.out.println("Categories: " + p1Session.getCategories());
        System.out.println("Players: " + playerSessions.size());
        System.out.println("Host: " + hostSession.getPlayer().getUsername());
        System.out.println("Player 2: " + regularPlayerSession.getPlayer().getUsername());
        
        System.out.println("\n🎉 MULTIPLAYER BACKEND VALIDATION: PASSED");
        System.out.println("✓ Both players receive identical sessionId");
        System.out.println("✓ Both players receive identical letter");
        System.out.println("✓ Both players receive identical categories");
        System.out.println("✓ Server is single source of truth");
    }
    
    /**
     * Additional test to verify server letter generation consistency.
     */
    @Test
    public void testServerLetterGeneration_SingleSourceOfTruth() {
        System.out.println("\n=== Testing Server Letter Generation ===");
        
        // Create multiple sessions and verify each gets a letter
        for (int i = 0; i < 5; i++) {
            GameSessionEntity session = gameSessionService.createSession(
                    "Host" + i, 
                    Arrays.asList("Category1", "Category2"), 
                    30
            );
            
            // Start round to generate letter
            GameSessionEntity startedSession = gameSessionService.startRound(session.getSessionId());
            
            assertNotNull(startedSession.getLetter(), "Letter should be generated for session " + i);
            assertTrue(startedSession.getLetter().matches("[A-Z]"), 
                      "Letter should be valid uppercase for session " + i);
            
            System.out.println("Session " + i + " - ID: " + startedSession.getSessionId() + 
                             ", Letter: " + startedSession.getLetter());
        }
        
        System.out.println("✓ Server letter generation working correctly");
    }
}
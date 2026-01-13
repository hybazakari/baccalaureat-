package com.bac_game_server.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * WebSocket configuration for multiplayer game communication.
 * Enables real-time bidirectional communication between clients and server.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final MultiplayerGameHandler gameHandler;

    @Autowired
    public WebSocketConfig(MultiplayerGameHandler gameHandler) {
        this.gameHandler = gameHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Register the multiplayer game handler for WebSocket connections
        registry.addHandler(gameHandler, "/websocket")
                .setAllowedOrigins("*"); // Allow cross-origin for development
    }
}
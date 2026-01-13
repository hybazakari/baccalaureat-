package com.bac_game_server.exception;

/**
 * Exception thrown when attempting to join a room that cannot accept more players.
 * This can occur when the room is full or in a state that doesn't allow new players.
 */
public class RoomFullException extends RuntimeException {

    private final String roomCode;
    private final int maxCapacity;
    private final int currentPlayerCount;

    public RoomFullException(String roomCode) {
        super("Room '" + roomCode + "' is full and cannot accept more players");
        this.roomCode = roomCode;
        this.maxCapacity = -1;
        this.currentPlayerCount = -1;
    }

    public RoomFullException(String roomCode, int maxCapacity, int currentPlayerCount) {
        super("Room '" + roomCode + "' is full (" + currentPlayerCount + "/" + maxCapacity + ")");
        this.roomCode = roomCode;
        this.maxCapacity = maxCapacity;
        this.currentPlayerCount = currentPlayerCount;
    }

    public RoomFullException(String roomCode, String message) {
        super(message);
        this.roomCode = roomCode;
        this.maxCapacity = -1;
        this.currentPlayerCount = -1;
    }

    public RoomFullException(String roomCode, String message, Throwable cause) {
        super(message, cause);
        this.roomCode = roomCode;
        this.maxCapacity = -1;
        this.currentPlayerCount = -1;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public int getCurrentPlayerCount() {
        return currentPlayerCount;
    }
}
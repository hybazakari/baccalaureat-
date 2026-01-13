package com.bac_game_server.exception;

/**
 * Exception thrown when a requested game room is not found.
 */
public class RoomNotFoundException extends RuntimeException {

    private final String roomCode;

    public RoomNotFoundException(String roomCode) {
        super("Room with code '" + roomCode + "' not found");
        this.roomCode = roomCode;
    }

    public RoomNotFoundException(String roomCode, String message) {
        super(message);
        this.roomCode = roomCode;
    }

    public RoomNotFoundException(String roomCode, String message, Throwable cause) {
        super(message, cause);
        this.roomCode = roomCode;
    }

    public String getRoomCode() {
        return roomCode;
    }
}
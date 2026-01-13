package com.bac_game_server.exception;

/**
 * Exception thrown when attempting to join a room that is not in a joinable state.
 * This typically occurs when trying to join a room that is already running or finished.
 */
public class RoomNotJoinableException extends RuntimeException {

    private final String roomCode;
    private final String roomStatus;

    public RoomNotJoinableException(String roomCode, String roomStatus) {
        super("Cannot join room '" + roomCode + "' - room is " + roomStatus);
        this.roomCode = roomCode;
        this.roomStatus = roomStatus;
    }

    public RoomNotJoinableException(String roomCode, String roomStatus, String message) {
        super(message);
        this.roomCode = roomCode;
        this.roomStatus = roomStatus;
    }

    public RoomNotJoinableException(String roomCode, String roomStatus, String message, Throwable cause) {
        super(message, cause);
        this.roomCode = roomCode;
        this.roomStatus = roomStatus;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public String getRoomStatus() {
        return roomStatus;
    }
}
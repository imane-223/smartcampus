package com.smartcampus.room.exception;

public class BatimentConflictException extends RuntimeException {
    public BatimentConflictException() {
        super("Un bâtiment porte déjà ce nom.");
    }
}

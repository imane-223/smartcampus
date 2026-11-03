package com.smartcampus.room.exception;

public class CodeEspaceConflictException extends RuntimeException {
    public CodeEspaceConflictException() {
        super("Un espace utilise déjà ce code.");
    }
}

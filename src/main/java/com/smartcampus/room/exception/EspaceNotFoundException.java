package com.smartcampus.room.exception;

public class EspaceNotFoundException extends RuntimeException {
    public EspaceNotFoundException(Long id) {
        super("L'espace demandé est introuvable.");
    }
}

package com.smartcampus.room.exception;

public class BatimentNotFoundException extends RuntimeException {
    public BatimentNotFoundException(Long id) {
        super("Le bâtiment indiqué est introuvable.");
    }
}

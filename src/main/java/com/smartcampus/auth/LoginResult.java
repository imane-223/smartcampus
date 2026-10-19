package com.smartcampus.auth;

public enum LoginResult {
    SUCCESS("Connexion reussie"),
    INVALID_CREDENTIALS("E-mail ou mot de passe incorrect"),
    INVALID_EMAIL_FORMAT("L'e-mail doit etre une adresse institutionnelle"),
    ACCOUNT_DISABLED("Ce compte est desactive");

    private final String message;

    LoginResult(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
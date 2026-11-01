package com.smartcampus.auth.service;

public enum LoginResult {
    SUCCESS("Connexion réussie."),
    INVALID_CREDENTIALS("E-mail ou mot de passe incorrect."),
    INVALID_EMAIL_FORMAT("L'e-mail doit être une adresse institutionnelle."),
    ACCOUNT_DISABLED("Ce compte est désactivé."),
    // Le délai restant est ajouté par LoginOutcome, d'après l'heure de déverrouillage réelle
    ACCOUNT_LOCKED("Compte temporairement verrouillé après plusieurs échecs.");

    private final String message;

    LoginResult(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}

package com.smartcampus.auth.service;

import java.time.Duration;
import java.time.Instant;

import com.smartcampus.auth.model.Utilisateur;

/**
 * Résultat d'une tentative de connexion. utilisateur n'est renseigné qu'en cas de succès,
 * lockedUntil et retryAfter seulement pour ACCOUNT_LOCKED.
 */
public record LoginOutcome(LoginResult result, Utilisateur utilisateur, Instant lockedUntil, Duration retryAfter) {

    static LoginOutcome of(LoginResult result) {
        return new LoginOutcome(result, null, null, null);
    }

    static LoginOutcome success(Utilisateur utilisateur) {
        return new LoginOutcome(LoginResult.SUCCESS, utilisateur, null, null);
    }

    static LoginOutcome locked(Instant lockedUntil, Instant now) {
        return new LoginOutcome(LoginResult.ACCOUNT_LOCKED, null, lockedUntil, Duration.between(now, lockedUntil));
    }

    public boolean isSuccess() {
        return result == LoginResult.SUCCESS;
    }

    /** Délai avant nouvel essai, en secondes arrondies au supérieur (en-tête Retry-After). */
    public long retryAfterSeconds() {
        return retryAfter.getSeconds() + (retryAfter.getNano() > 0 ? 1 : 0);
    }

    /** Message destiné à l'utilisateur. Durée relative, jamais d'heure locale. */
    public String message() {
        if (result != LoginResult.ACCOUNT_LOCKED) {
            return result.getMessage();
        }
        long minutes = Math.max(1, (retryAfterSeconds() + 59) / 60);
        return result.getMessage() + " Réessayez dans " + minutes + (minutes > 1 ? " minutes." : " minute.");
    }
}

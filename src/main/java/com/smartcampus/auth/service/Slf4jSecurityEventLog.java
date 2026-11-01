package com.smartcampus.auth.service;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Journal des événements de sécurité. Ne contient jamais de mot de passe. */
@Component
public class Slf4jSecurityEventLog implements SecurityEventLog {

    private static final Logger log = LoggerFactory.getLogger("com.smartcampus.security");

    @Override
    public void loginFailuresExceeded(String email, int failureCount) {
        log.warn("Échecs de connexion consécutifs : compte={} échecs={}", email, failureCount);
    }

    @Override
    public void accountLocked(String email, Instant lockedUntil) {
        log.warn("Compte verrouillé : compte={} jusqu'à={}", email, lockedUntil);
    }

    @Override
    public void unknownAccountLoginAttempt(String email) {
        log.info("Tentative de connexion sur un compte inconnu : compte={}", email);
    }
}

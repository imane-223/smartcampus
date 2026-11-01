package com.smartcampus.auth.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartcampus.auth.model.Role;
import com.smartcampus.auth.model.Utilisateur;
import com.smartcampus.auth.repository.UtilisateurRepository;

/**
 * Vérifie les identifiants. Règle unique pour l'état du compte : le mot de passe est
 * vérifié d'abord, et l'état (désactivé, verrouillé) n'est révélé que s'il est correct.
 * Un e-mail inconnu et un mauvais mot de passe donnent donc toujours la même réponse.
 */
@Service
@Transactional
public class AuthService {

    static final int FAILURES_BEFORE_LOG = 3;
    static final int FAILURES_BEFORE_LOCK = 5;
    static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final UtilisateurRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityEventLog securityLog;
    private final Clock clock;
    // Comparé pour un e-mail inconnu, afin que le temps de réponse ne révèle pas l'existence du compte
    private final String dummyHash;

    public AuthService(UtilisateurRepository repository, PasswordEncoder passwordEncoder,
            SecurityEventLog securityLog, Clock clock) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.securityLog = securityLog;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode("smartcampus-compte-inexistant");
    }

    public LoginOutcome login(String email, String password) {
        if (email == null || email.isBlank()) {
            return LoginOutcome.of(LoginResult.INVALID_EMAIL_FORMAT);
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        boolean institutional = Arrays.stream(Role.values())
                .anyMatch(role -> role.acceptsEmail(normalized));
        if (!institutional) {
            return LoginOutcome.of(LoginResult.INVALID_EMAIL_FORMAT);
        }
        if (password == null || password.isEmpty()) {
            return LoginOutcome.of(LoginResult.INVALID_CREDENTIALS);
        }

        Optional<Utilisateur> found = repository.findByCourrielIgnoreCase(normalized);
        if (found.isEmpty()) {
            passwordEncoder.matches(password, dummyHash);
            securityLog.unknownAccountLoginAttempt(normalized);
            return LoginOutcome.of(LoginResult.INVALID_CREDENTIALS);
        }
        Utilisateur user = found.get();
        Instant now = clock.instant();

        // Un verrou expiré est effacé avant toute autre vérification
        if (user.getVerrouilleJusquA() != null && !user.getVerrouilleJusquA().isAfter(now)) {
            user.setVerrouilleJusquA(null);
            user.setTentativesEchouees(0);
        }
        boolean locked = user.getVerrouilleJusquA() != null;

        if (!passwordEncoder.matches(password, user.getMotDePasseHash())) {
            // Pendant le verrouillage : ni incrément ni nouvel événement
            if (!locked) {
                registerFailure(user, now);
            }
            return LoginOutcome.of(LoginResult.INVALID_CREDENTIALS);
        }
        if (locked) {
            // Mot de passe correct : on révèle le verrou, sans le lever ni remettre le compteur à zéro
            return LoginOutcome.locked(user.getVerrouilleJusquA(), now);
        }
        if (!user.isActif()) {
            return LoginOutcome.of(LoginResult.ACCOUNT_DISABLED);
        }

        user.setTentativesEchouees(0);
        return LoginOutcome.success(user);
    }

    private void registerFailure(Utilisateur user, Instant now) {
        int count = user.getTentativesEchouees() + 1;
        user.setTentativesEchouees(count);
        if (count >= FAILURES_BEFORE_LOG) {
            securityLog.loginFailuresExceeded(user.getCourriel(), count);
        }
        if (count >= FAILURES_BEFORE_LOCK) {
            Instant until = now.plus(LOCK_DURATION);
            user.setVerrouilleJusquA(until);
            securityLog.accountLocked(user.getCourriel(), until);
        }
    }
}

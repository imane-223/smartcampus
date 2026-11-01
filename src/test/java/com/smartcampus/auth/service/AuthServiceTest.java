package com.smartcampus.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.smartcampus.auth.model.Enseignant;
import com.smartcampus.auth.model.Etudiant;
import com.smartcampus.auth.model.Utilisateur;
import com.smartcampus.auth.repository.UtilisateurRepository;

class AuthServiceTest {

    private static class RecordingLog implements SecurityEventLog {
        int calls = 0;
        String lastEmail;
        int lastCount;
        int locks = 0;

        @Override
        public void loginFailuresExceeded(String email, int failureCount) {
            calls++;
            lastEmail = email;
            lastCount = failureCount;
        }

        @Override
        public void accountLocked(String email, Instant lockedUntil) {
            locks++;
        }

        @Override
        public void unknownAccountLoginAttempt(String email) {
        }
    }

    /** Horloge que le test fait avancer, pour éprouver l'expiration du verrou sans attendre. */
    private static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-11-02T08:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }
    }

    private static final String STUDENT = "etudiant@usms.ac.ma";
    private static final String TEACHER = "prof@usms.ma";
    private static final String DISABLED = "ancien@usms.ac.ma";
    private static final String PASSWORD = "Secret123";

    // Coût réduit : les tests vérifient la logique, pas la robustesse de BCrypt
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final Map<String, Utilisateur> users = new HashMap<>();
    private final MutableClock clock = new MutableClock();
    private RecordingLog log;
    private AuthService service;

    @BeforeEach
    void setUp() {
        log = new RecordingLog();
        UtilisateurRepository repository = mock(UtilisateurRepository.class);
        when(repository.findByCourrielIgnoreCase(anyString()))
                .thenAnswer(call -> Optional.ofNullable(users.get(call.<String>getArgument(0).toLowerCase(Locale.ROOT))));
        service = new AuthService(repository, encoder, log, clock);

        add(new Etudiant(STUDENT, encoder.encode(PASSWORD), "Sara", "Alaoui"));
        add(new Enseignant(TEACHER, encoder.encode(PASSWORD), "Karim", "Bennani"));
        Etudiant disabled = new Etudiant(DISABLED, encoder.encode(PASSWORD), "Omar", "Tazi");
        disabled.setActif(false);
        add(disabled);
    }

    private void add(Utilisateur user) {
        users.put(user.getCourriel(), user);
    }

    private Utilisateur student() {
        return users.get(STUDENT);
    }

    private void failTimes(int times) {
        for (int i = 0; i < times; i++) {
            service.login(STUDENT, "faux" + i);
        }
    }

    // --- Cas repris de LoginServiceTest ---

    @Test
    void correctCredentialsGiveSuccess() {
        LoginOutcome outcome = service.login(STUDENT, PASSWORD);
        assertThat(outcome.result()).isEqualTo(LoginResult.SUCCESS);
        assertThat(outcome.utilisateur()).isSameAs(student());
        assertThat(service.login(TEACHER, PASSWORD).result()).isEqualTo(LoginResult.SUCCESS);
    }

    @Test
    void wrongPasswordGivesInvalidCredentials() {
        assertThat(service.login(STUDENT, "mauvais").result()).isEqualTo(LoginResult.INVALID_CREDENTIALS);
    }

    @Test
    void unknownEmailGivesSameResultAsWrongPassword() {
        LoginOutcome unknown = service.login("inconnu@usms.ac.ma", PASSWORD);
        LoginOutcome wrong = service.login(STUDENT, "mauvais");
        assertThat(unknown.result()).isEqualTo(LoginResult.INVALID_CREDENTIALS);
        assertThat(unknown.message()).isEqualTo(wrong.message());
    }

    @Test
    void nonInstitutionalEmailIsRejected() {
        assertThat(service.login("quelquun@gmail.com", PASSWORD).result())
                .isEqualTo(LoginResult.INVALID_EMAIL_FORMAT);
    }

    @Test
    void disabledAccountIsRefused() {
        assertThat(service.login(DISABLED, PASSWORD).result()).isEqualTo(LoginResult.ACCOUNT_DISABLED);
    }

    @Test
    void disabledAccountWithWrongPasswordLooksLikeAnyFailure() {
        assertThat(service.login(DISABLED, "mauvais").result()).isEqualTo(LoginResult.INVALID_CREDENTIALS);
    }

    @Test
    void threeConsecutiveFailuresAreLogged() {
        service.login(STUDENT, "faux1");
        service.login(STUDENT, "faux2");
        assertThat(log.calls).isZero();

        service.login(STUDENT, "faux3");
        assertThat(log.calls).isEqualTo(1);
        assertThat(log.lastEmail).isEqualTo(STUDENT);
        assertThat(log.lastCount).isEqualTo(3);
    }

    @Test
    void successResetsTheFailureCounter() {
        service.login(STUDENT, "faux1");
        service.login(STUDENT, "faux2");
        service.login(STUDENT, PASSWORD);
        service.login(STUDENT, "faux3");
        assertThat(log.calls).isZero();
    }

    // --- Mot de passe absent (remarque de revue PR #1) ---

    @Test
    void nullOrEmptyPasswordGivesInvalidCredentials() {
        assertThat(service.login(STUDENT, null).result()).isEqualTo(LoginResult.INVALID_CREDENTIALS);
        assertThat(service.login(STUDENT, "").result()).isEqualTo(LoginResult.INVALID_CREDENTIALS);
    }

    // --- Verrouillage ---

    @Test
    void fifthFailureLocksTheAccountForFifteenMinutes() {
        failTimes(4);
        assertThat(student().getVerrouilleJusquA()).isNull();

        failTimes(1);
        assertThat(student().getTentativesEchouees()).isEqualTo(5);
        assertThat(student().getVerrouilleJusquA()).isEqualTo(clock.now.plus(Duration.ofMinutes(15)));
        assertThat(log.locks).isEqualTo(1);
    }

    @Test
    void expiredLockIsClearedBeforeAnyOtherCheck() {
        failTimes(5);
        clock.advance(Duration.ofMinutes(15));

        service.login(STUDENT, "encore-faux");

        assertThat(student().getVerrouilleJusquA()).isNull();
        assertThat(student().getTentativesEchouees()).isEqualTo(1);
    }

    @Test
    void wrongPasswordDuringLockNeitherCountsNorLogs() {
        failTimes(5);
        int eventsBefore = log.calls;
        Instant lockedUntil = student().getVerrouilleJusquA();

        LoginOutcome outcome = service.login(STUDENT, "faux-pendant-verrou");

        assertThat(outcome.result()).isEqualTo(LoginResult.INVALID_CREDENTIALS);
        assertThat(student().getTentativesEchouees()).isEqualTo(5);
        assertThat(student().getVerrouilleJusquA()).isEqualTo(lockedUntil);
        assertThat(log.calls).isEqualTo(eventsBefore);
        assertThat(log.locks).isEqualTo(1);
    }

    @Test
    void correctPasswordDuringLockRevealsTheLockWithoutLiftingIt() {
        failTimes(5);
        Instant lockedUntil = student().getVerrouilleJusquA();
        clock.advance(Duration.ofMinutes(3).plusSeconds(10));

        LoginOutcome outcome = service.login(STUDENT, PASSWORD);

        assertThat(outcome.result()).isEqualTo(LoginResult.ACCOUNT_LOCKED);
        assertThat(outcome.lockedUntil()).isEqualTo(lockedUntil);
        // 11 min 50 s restantes : arrondi à la minute supérieure, durée relative
        assertThat(outcome.message())
                .isEqualTo("Compte temporairement verrouillé après plusieurs échecs. Réessayez dans 12 minutes.");
        assertThat(outcome.retryAfterSeconds()).isEqualTo(710);
        assertThat(student().getVerrouilleJusquA()).isEqualTo(lockedUntil);
        assertThat(student().getTentativesEchouees()).isEqualTo(5);
    }

    @Test
    void lockedAccountWithWrongPasswordLooksLikeUnknownAccount() {
        failTimes(5);
        LoginOutcome locked = service.login(STUDENT, "mauvais");
        LoginOutcome unknown = service.login("inconnu@usms.ac.ma", "mauvais");
        assertThat(locked.result()).isEqualTo(unknown.result());
        assertThat(locked.message()).isEqualTo(unknown.message());
    }

    @Test
    void successOnUnlockedAccountResetsTheCounterToZero() {
        failTimes(4);
        assertThat(student().getTentativesEchouees()).isEqualTo(4);

        assertThat(service.login(STUDENT, PASSWORD).result()).isEqualTo(LoginResult.SUCCESS);
        assertThat(student().getTentativesEchouees()).isZero();
    }

    @Test
    void lastMinuteIsSingular() {
        failTimes(5);
        clock.advance(Duration.ofMinutes(14).plusSeconds(30));
        assertThat(service.login(STUDENT, PASSWORD).message()).endsWith("Réessayez dans 1 minute.");
    }
}

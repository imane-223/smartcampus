package com.smartcampus.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class LoginServiceTest {

    private static class RecordingLog implements SecurityEventLog {
        int calls = 0;
        String lastEmail;
        int lastCount;

        @Override
        public void loginFailuresExceeded(String email, int failureCount) {
            calls++;
            lastEmail = email;
            lastCount = failureCount;
        }
    }

    private static final String STUDENT = "etudiant@usms.ac.ma";
    private static final String TEACHER = "prof@usms.ma";
    private static final String DISABLED = "ancien@usms.ac.ma";
    private static final String PASSWORD = "Secret123";

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private RecordingLog log;
    private LoginService service;

    @BeforeEach
    void setUp() {
        log = new RecordingLog();
        service = new LoginService(encoder, log);
        service.addUser(new User(STUDENT, encoder.encode(PASSWORD), Role.STUDENT, true));
        service.addUser(new User(TEACHER, encoder.encode(PASSWORD), Role.TEACHER, true));
        service.addUser(new User(DISABLED, encoder.encode(PASSWORD), Role.STUDENT, false));
    }

    @Test
    void correctCredentialsGiveSuccess() {
        assertThat(service.login(STUDENT, PASSWORD)).isEqualTo(LoginResult.SUCCESS);
        assertThat(service.login(TEACHER, PASSWORD)).isEqualTo(LoginResult.SUCCESS);
    }

    @Test
    void wrongPasswordGivesInvalidCredentials() {
        assertThat(service.login(STUDENT, "mauvais")).isEqualTo(LoginResult.INVALID_CREDENTIALS);
    }

    @Test
    void unknownEmailGivesSameResultAsWrongPassword() {
        assertThat(service.login("inconnu@usms.ac.ma", PASSWORD))
                .isEqualTo(LoginResult.INVALID_CREDENTIALS);
    }

    @Test
    void nonInstitutionalEmailIsRejected() {
        assertThat(service.login("quelquun@gmail.com", PASSWORD))
                .isEqualTo(LoginResult.INVALID_EMAIL_FORMAT);
    }

    @Test
    void disabledAccountIsRefused() {
        assertThat(service.login(DISABLED, PASSWORD)).isEqualTo(LoginResult.ACCOUNT_DISABLED);
    }

    @Test
    void emailDomainMustMatchRole() {
        User wrongDomain = new User("etudiant2@usms.ma", encoder.encode(PASSWORD), Role.STUDENT, true);
        assertThatThrownBy(() -> service.addUser(wrongDomain))
                .isInstanceOf(IllegalArgumentException.class);
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
        assertThat(log.calls).isEqualTo(99);
    }
}
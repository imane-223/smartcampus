package com.smartcampus.auth;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.crypto.password.PasswordEncoder;

public class LoginService {

    static final int MAX_FAILURES = 3;

    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, Integer> failures = new ConcurrentHashMap<>();
    private final PasswordEncoder passwordEncoder;
    private final SecurityEventLog securityLog;

    public LoginService(PasswordEncoder passwordEncoder, SecurityEventLog securityLog) {
        this.passwordEncoder = passwordEncoder;
        this.securityLog = securityLog;
    }

    public void addUser(User user) {
        if (!user.role().acceptsEmail(user.email())) {
            throw new IllegalArgumentException(
                    "Le domaine de l'e-mail ne correspond pas au role " + user.role());
        }
        users.put(user.email().toLowerCase(), user);
    }

    public LoginResult login(String email, String password) {
        if (email == null || email.isBlank() || password == null) {
            return LoginResult.INVALID_EMAIL_FORMAT;
        }
        String normalized = email.trim().toLowerCase();

        boolean institutional = Arrays.stream(Role.values())
                .anyMatch(role -> role.acceptsEmail(normalized));
        if (!institutional) {
            return LoginResult.INVALID_EMAIL_FORMAT;
        }

        User user = users.get(normalized);
        if (user == null || !passwordEncoder.matches(password, user.passwordHash())) {
            registerFailure(normalized);
            return LoginResult.INVALID_CREDENTIALS;
        }
        if (!user.active()) {
            return LoginResult.ACCOUNT_DISABLED;
        }

        failures.remove(normalized);
        return LoginResult.SUCCESS;
    }

    private void registerFailure(String email) {
        int count = failures.merge(email, 1, Integer::sum);
        if (count >= MAX_FAILURES) {
            securityLog.loginFailuresExceeded(email, count);
        }
    }
}
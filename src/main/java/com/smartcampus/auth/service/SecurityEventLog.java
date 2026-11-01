package com.smartcampus.auth.service;

import java.time.Instant;

public interface SecurityEventLog {

    void loginFailuresExceeded(String email, int failureCount);

    void accountLocked(String email, Instant lockedUntil);

    void unknownAccountLoginAttempt(String email);
}

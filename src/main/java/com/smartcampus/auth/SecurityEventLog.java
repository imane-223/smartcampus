package com.smartcampus.auth;

public interface SecurityEventLog {

    void loginFailuresExceeded(String email, int failureCount);
}
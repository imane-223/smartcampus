package com.smartcampus.auth.service;

public interface SecurityEventLog {

    void loginFailuresExceeded(String email, int failureCount);
}
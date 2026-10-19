package com.smartcampus.auth;

public record User(String email, String passwordHash, Role role, boolean active) {
}
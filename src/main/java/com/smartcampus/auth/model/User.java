package com.smartcampus.auth.model;

public record User(String email, String passwordHash, Role role, boolean active) {
}
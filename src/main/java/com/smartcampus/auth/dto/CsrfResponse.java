package com.smartcampus.auth.dto;

/** Jeton CSRF à renvoyer dans l'en-tête headerName sur chaque POST, PUT, PATCH et DELETE. */
public record CsrfResponse(String headerName, String token) {
}

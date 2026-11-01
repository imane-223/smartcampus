package com.smartcampus.common;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Format JSON commun à toutes les erreurs de l'API (voir docs/conventions.md).
 * fields : seulement pour les erreurs de validation ; retryAt : instant (UTC) à partir duquel réessayer.
 */
public record ErrorResponse(
        int status,
        String error,
        String message,
        String path,
        Instant timestamp,
        @JsonInclude(JsonInclude.Include.NON_NULL) Map<String, String> fields,
        @JsonInclude(JsonInclude.Include.NON_NULL) Instant retryAt) {

    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(status, error, message, path, Instant.now(), null, null);
    }

    public ErrorResponse withFields(Map<String, String> fields) {
        return new ErrorResponse(status, error, message, path, timestamp, fields, retryAt);
    }

    public ErrorResponse withRetryAt(Instant retryAt) {
        return new ErrorResponse(status, error, message, path, timestamp, fields, retryAt);
    }
}

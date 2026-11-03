package com.smartcampus.room.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.smartcampus.common.ErrorResponse;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice(basePackages = "com.smartcampus.room")
public class EspaceExceptionHandler {

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErrorResponse> invalidParameter(MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ErrorResponse.of(400, "VALIDATION",
                "Un paramètre de recherche contient une valeur invalide.", request.getRequestURI())
                .withFields(Map.of(ex.getName(), "La valeur fournie est invalide.")));
    }

    @ExceptionHandler(EspaceNotFoundException.class)
    ResponseEntity<ErrorResponse> notFound(EspaceNotFoundException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ErrorResponse.of(404, "NOT_FOUND", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(BatimentNotFoundException.class)
    ResponseEntity<ErrorResponse> batimentNotFound(BatimentNotFoundException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ErrorResponse.of(404, "NOT_FOUND", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(BatimentConflictException.class)
    ResponseEntity<ErrorResponse> batimentConflict(BatimentConflictException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ErrorResponse.of(409, "CONFLICT", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(CodeEspaceConflictException.class)
    ResponseEntity<ErrorResponse> conflict(CodeEspaceConflictException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ErrorResponse.of(409, "CONFLICT", ex.getMessage(), request.getRequestURI()));
    }
}

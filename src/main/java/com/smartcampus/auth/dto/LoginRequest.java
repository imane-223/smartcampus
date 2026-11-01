package com.smartcampus.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "L'e-mail est obligatoire.") String courriel,
        @NotBlank(message = "Le mot de passe est obligatoire.") String motDePasse) {
}

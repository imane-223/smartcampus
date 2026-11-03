package com.smartcampus.room.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BatimentCreateRequest(
        @NotBlank(message = "Le nom du bâtiment est obligatoire.")
        @Size(max = 150, message = "Le nom du bâtiment ne peut pas dépasser 150 caractères.") String nom) {
}

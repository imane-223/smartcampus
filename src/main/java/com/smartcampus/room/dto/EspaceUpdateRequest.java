package com.smartcampus.room.dto;

import com.smartcampus.room.model.StatutEspace;
import com.smartcampus.room.model.TypeEspace;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** PUT complet : toutes les propriétés requises sont remplacées par celles de la requête. */
public record EspaceUpdateRequest(
        @NotBlank(message = "Le code est obligatoire.") @Size(max = 50, message = "Le code ne peut pas dépasser 50 caractères.") String code,
        @NotBlank(message = "Le nom est obligatoire.") @Size(max = 150, message = "Le nom ne peut pas dépasser 150 caractères.") String nom,
        @NotNull(message = "Le type est obligatoire.") TypeEspace type,
        @Positive(message = "La capacité doit être un entier strictement positif.") int capacite,
        @NotNull(message = "Le bâtiment est obligatoire.") Long batimentId,
        Integer etage,
        @Size(max = 2000, message = "La description ne peut pas dépasser 2000 caractères.") String description,
        @NotNull(message = "Le statut est obligatoire.") StatutEspace statut,
        @NotNull(message = "L'indicateur d'archivage est obligatoire.") Boolean archive) {
}

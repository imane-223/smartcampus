package com.smartcampus.auth.dto;

import com.smartcampus.auth.model.Role;
import com.smartcampus.auth.model.Utilisateur;

public record UserResponse(Long id, String prenom, String nom, String courriel, Role role) {

    public static UserResponse from(Utilisateur utilisateur) {
        return new UserResponse(utilisateur.getId(), utilisateur.getPrenom(), utilisateur.getNom(),
                utilisateur.getCourriel(), utilisateur.getRole());
    }
}

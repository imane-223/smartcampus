package com.smartcampus.auth.security;

import java.io.Serializable;

import com.smartcampus.auth.model.Role;

/**
 * Utilisateur connecté, stocké en session comme principal Spring Security.
 * On n'y met jamais l'entité JPA : seulement ce qu'il faut pour les autorisations.
 * Récupérable dans un contrôleur par @AuthenticationPrincipal AuthenticatedUser.
 */
public record AuthenticatedUser(Long id, String courriel, Role role) implements Serializable {
}

package com.smartcampus.auth.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("ADMIN")
public class Administrateur extends Utilisateur {

    protected Administrateur() {
    }

    public Administrateur(String courriel, String motDePasseHash, String prenom, String nom) {
        super(courriel, motDePasseHash, prenom, nom);
    }

    @Override
    public Role getRole() {
        return Role.ADMIN;
    }
}

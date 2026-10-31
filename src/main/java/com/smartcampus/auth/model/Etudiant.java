package com.smartcampus.auth.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("STUDENT")
public class Etudiant extends Utilisateur {

    private String numero;
    private String codeMassar;
    private String filiere;

    protected Etudiant() {
    }

    public Etudiant(String courriel, String motDePasseHash, String prenom, String nom) {
        super(courriel, motDePasseHash, prenom, nom);
    }

    @Override
    public Role getRole() {
        return Role.STUDENT;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getCodeMassar() {
        return codeMassar;
    }

    public void setCodeMassar(String codeMassar) {
        this.codeMassar = codeMassar;
    }

    public String getFiliere() {
        return filiere;
    }

    public void setFiliere(String filiere) {
        this.filiere = filiere;
    }
}

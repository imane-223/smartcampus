package com.smartcampus.auth.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("TEACHER")
public class Enseignant extends Utilisateur {

    private String numero;
    private String codeEnseignant;
    private String grade;
    private String specialite;

    protected Enseignant() {
    }

    public Enseignant(String courriel, String motDePasseHash, String prenom, String nom) {
        super(courriel, motDePasseHash, prenom, nom);
    }

    @Override
    public Role getRole() {
        return Role.TEACHER;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getCodeEnseignant() {
        return codeEnseignant;
    }

    public void setCodeEnseignant(String codeEnseignant) {
        this.codeEnseignant = codeEnseignant;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public String getSpecialite() {
        return specialite;
    }

    public void setSpecialite(String specialite) {
        this.specialite = specialite;
    }
}

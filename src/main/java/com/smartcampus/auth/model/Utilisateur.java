package com.smartcampus.auth.model;

import java.time.Instant;
import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Compte SmartCampus. Les trois roles partagent la table "utilisateurs" ;
 * la colonne "role" est le discriminateur et n'est donc pas mappee comme champ.
 */
@Entity
@Table(name = "utilisateurs")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "role")
public abstract class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String courriel;

    @Column(nullable = false)
    private String motDePasseHash;

    @Column(nullable = false)
    private String prenom;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private boolean actif = true;

    @Column(nullable = false)
    private boolean changerMotDePasse;

    @Column(nullable = false)
    private int tentativesEchouees;

    // Nom explicite : la strategie par defaut produirait "verrouille_jusqua"
    @Column(name = "verrouille_jusqu_a")
    private Instant verrouilleJusquA;

    @Column(nullable = false, updatable = false)
    private Instant creeLe;

    protected Utilisateur() {
    }

    protected Utilisateur(String courriel, String motDePasseHash, String prenom, String nom) {
        this.courriel = courriel;
        this.motDePasseHash = motDePasseHash;
        this.prenom = prenom;
        this.nom = nom;
    }

    public abstract Role getRole();

    @PrePersist
    void beforeInsert() {
        if (creeLe == null) {
            creeLe = Instant.now();
        }
        normalizeAndCheckEmail();
    }

    @PreUpdate
    void beforeUpdate() {
        normalizeAndCheckEmail();
    }

    private void normalizeAndCheckEmail() {
        if (courriel == null) {
            throw new IllegalStateException("Le courriel est obligatoire.");
        }
        courriel = courriel.trim().toLowerCase(Locale.ROOT);
        if (!getRole().acceptsEmail(courriel)) {
            throw new IllegalStateException("Le courriel " + courriel
                    + " ne correspond pas au domaine @" + getRole().getEmailDomain() + " du rôle " + getRole() + ".");
        }
    }

    public Long getId() {
        return id;
    }

    public String getCourriel() {
        return courriel;
    }

    public void setCourriel(String courriel) {
        this.courriel = courriel;
    }

    public String getMotDePasseHash() {
        return motDePasseHash;
    }

    public void setMotDePasseHash(String motDePasseHash) {
        this.motDePasseHash = motDePasseHash;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }

    public boolean isChangerMotDePasse() {
        return changerMotDePasse;
    }

    public void setChangerMotDePasse(boolean changerMotDePasse) {
        this.changerMotDePasse = changerMotDePasse;
    }

    public int getTentativesEchouees() {
        return tentativesEchouees;
    }

    public void setTentativesEchouees(int tentativesEchouees) {
        this.tentativesEchouees = tentativesEchouees;
    }

    public Instant getVerrouilleJusquA() {
        return verrouilleJusquA;
    }

    public void setVerrouilleJusquA(Instant verrouilleJusquA) {
        this.verrouilleJusquA = verrouilleJusquA;
    }

    public Instant getCreeLe() {
        return creeLe;
    }
}

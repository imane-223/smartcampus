package com.smartcampus.room.model;

import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "espaces", uniqueConstraints = @UniqueConstraint(name = "uk_espaces_code", columnNames = "code"))
public class Espace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String nom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TypeEspace type;

    @Column(nullable = false)
    private int capacite;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batiment_id", nullable = false)
    private Batiment batiment;

    private Integer etage;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutEspace statut = StatutEspace.ACTIVE;

    @Column(nullable = false)
    private boolean archive;

    protected Espace() {
    }

    public Espace(String code, String nom, TypeEspace type, int capacite, Batiment batiment,
            Integer etage, String description, StatutEspace statut, boolean archive) {
        this.code = normalizeCode(code);
        this.nom = nom == null ? null : nom.trim();
        this.type = type;
        this.capacite = capacite;
        this.batiment = batiment;
        this.etage = etage;
        this.description = description == null || description.isBlank() ? null : description.trim();
        this.statut = statut == null ? StatutEspace.ACTIVE : statut;
        this.archive = archive;
    }

    public void replace(String code, String nom, TypeEspace type, int capacite, Batiment batiment,
            Integer etage, String description, StatutEspace statut, boolean archive) {
        this.code = normalizeCode(code);
        this.nom = nom.trim();
        this.type = type;
        this.capacite = capacite;
        this.batiment = batiment;
        this.etage = etage;
        this.description = description == null || description.isBlank() ? null : description.trim();
        this.statut = statut;
        this.archive = archive;
    }

    private static String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
    }

    public boolean isReservable() {
        return !archive && statut == StatutEspace.ACTIVE;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getNom() { return nom; }
    public TypeEspace getType() { return type; }
    public int getCapacite() { return capacite; }
    public Batiment getBatiment() { return batiment; }
    public Integer getEtage() { return etage; }
    public String getDescription() { return description; }
    public StatutEspace getStatut() { return statut; }
    public boolean isArchive() { return archive; }
}

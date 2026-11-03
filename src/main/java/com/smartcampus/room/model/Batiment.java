package com.smartcampus.room.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Référence minimale partagée par les espaces du campus. */
@Entity
@Table(name = "batiments")
public class Batiment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String nom;

    protected Batiment() {
    }

    public Batiment(String nom) {
        this.nom = nom == null ? null : nom.trim();
    }

    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }
}

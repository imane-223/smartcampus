package com.smartcampus.room.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartcampus.room.model.Batiment;

public interface BatimentRepository extends JpaRepository<Batiment, Long> {
    boolean existsByNomIgnoreCase(String nom);
}

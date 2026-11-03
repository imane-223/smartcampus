package com.smartcampus.room.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.smartcampus.room.model.Espace;

public interface EspaceRepository extends JpaRepository<Espace, Long>, JpaSpecificationExecutor<Espace> {
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
    Optional<Espace> findByIdAndArchiveFalse(Long id);
}

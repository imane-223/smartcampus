package com.smartcampus.room.service;

import java.util.List;
import java.sql.SQLException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartcampus.room.dto.BatimentResponse;
import com.smartcampus.room.dto.BatimentCreateRequest;
import com.smartcampus.room.exception.BatimentConflictException;
import com.smartcampus.room.model.Batiment;
import com.smartcampus.room.repository.BatimentRepository;

@Service
@Transactional(readOnly = true)
public class BatimentService {
    private final BatimentRepository repository;

    public BatimentService(BatimentRepository repository) {
        this.repository = repository;
    }

    public List<BatimentResponse> lister() {
        return repository.findAll().stream().map(BatimentResponse::from).toList();
    }

    @Transactional
    public BatimentResponse creer(BatimentCreateRequest request) {
        String nom = request.nom().trim();
        if (repository.existsByNomIgnoreCase(nom)) {
            throw new BatimentConflictException();
        }
        try {
            return BatimentResponse.from(repository.saveAndFlush(new Batiment(nom)));
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            if (ex.getMostSpecificCause() instanceof SQLException sqlException
                    && "23505".equals(sqlException.getSQLState())) {
                throw new BatimentConflictException();
            }
            throw ex;
        }
    }
}

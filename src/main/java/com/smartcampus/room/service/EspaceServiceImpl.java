package com.smartcampus.room.service;

import java.util.Locale;
import java.sql.SQLException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartcampus.room.dto.EspaceCreateRequest;
import com.smartcampus.room.dto.EspaceResponse;
import com.smartcampus.room.dto.EspaceUpdateRequest;
import com.smartcampus.room.exception.BatimentNotFoundException;
import com.smartcampus.room.exception.CodeEspaceConflictException;
import com.smartcampus.room.exception.EspaceNotFoundException;
import com.smartcampus.room.model.Batiment;
import com.smartcampus.room.model.Espace;
import com.smartcampus.room.model.StatutEspace;
import com.smartcampus.room.model.TypeEspace;
import com.smartcampus.room.repository.BatimentRepository;
import com.smartcampus.room.repository.EspaceRepository;

@Service
@Transactional(readOnly = true)
public class EspaceServiceImpl implements EspaceService {

    private final EspaceRepository espaceRepository;
    private final BatimentRepository batimentRepository;

    public EspaceServiceImpl(EspaceRepository espaceRepository, BatimentRepository batimentRepository) {
        this.espaceRepository = espaceRepository;
        this.batimentRepository = batimentRepository;
    }

    @Override
    public Page<EspaceResponse> rechercher(TypeEspace type, Long batimentId, StatutEspace statut,
            String search, Pageable pageable) {
        return espaceRepository.findAll(EspaceSpecifications.catalog(type, batimentId, statut, search), pageable)
                .map(EspaceResponse::from);
    }

    @Override
    public EspaceResponse consulter(Long id) {
        return EspaceResponse.from(trouverVisible(id));
    }

    @Override
    @Transactional
    public EspaceResponse creer(EspaceCreateRequest request) {
        String code = normaliserCode(request.code());
        verifierCodeDisponible(code, null);
        Batiment batiment = trouverBatiment(request.batimentId());
        Espace espace = new Espace(code, request.nom(), request.type(), request.capacite(), batiment,
                request.etage(), request.description(), request.statut(), false);
        return enregistrer(espace);
    }

    @Override
    @Transactional
    public EspaceResponse modifier(Long id, EspaceUpdateRequest request) {
        Espace espace = trouver(id);
        String code = normaliserCode(request.code());
        verifierCodeDisponible(code, id);
        Batiment batiment = trouverBatiment(request.batimentId());
        espace.replace(code, request.nom(), request.type(), request.capacite(), batiment,
                request.etage(), request.description(), request.statut(), request.archive());
        return enregistrer(espace);
    }

    @Override
    public boolean existe(Long id) {
        return espaceRepository.existsById(id);
    }

    @Override
    public StatutEspace statutDe(Long id) {
        return trouver(id).getStatut();
    }

    @Override
    public boolean estArchive(Long id) {
        return trouver(id).isArchive();
    }

    @Override
    public boolean estReservable(Long id) {
        return trouver(id).isReservable();
    }

    private Espace trouverVisible(Long id) {
        return espaceRepository.findByIdAndArchiveFalse(id).orElseThrow(() -> new EspaceNotFoundException(id));
    }

    private Espace trouver(Long id) {
        return espaceRepository.findById(id).orElseThrow(() -> new EspaceNotFoundException(id));
    }

    private Batiment trouverBatiment(Long id) {
        return batimentRepository.findById(id).orElseThrow(() -> new BatimentNotFoundException(id));
    }

    private void verifierCodeDisponible(String code, Long idCourant) {
        boolean dejaUtilise = idCourant == null
                ? espaceRepository.existsByCodeIgnoreCase(code)
                : espaceRepository.existsByCodeIgnoreCaseAndIdNot(code, idCourant);
        if (dejaUtilise) {
            throw new CodeEspaceConflictException();
        }
    }

    private EspaceResponse enregistrer(Espace espace) {
        try {
            return EspaceResponse.from(espaceRepository.saveAndFlush(espace));
        } catch (DataIntegrityViolationException ex) {
            // La contrainte unique protège également contre deux créations concurrentes.
            if (ex.getMostSpecificCause() instanceof SQLException sqlException
                    && "23505".equals(sqlException.getSQLState())) {
                throw new CodeEspaceConflictException();
            }
            throw ex;
        }
    }

    private static String normaliserCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }
}

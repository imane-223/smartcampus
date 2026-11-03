package com.smartcampus.room.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.smartcampus.room.dto.EspaceCreateRequest;
import com.smartcampus.room.dto.EspaceResponse;
import com.smartcampus.room.dto.EspaceUpdateRequest;
import com.smartcampus.room.model.StatutEspace;
import com.smartcampus.room.model.TypeEspace;

public interface EspaceService {
    Page<EspaceResponse> rechercher(TypeEspace type, Long batimentId, StatutEspace statut, String search, Pageable pageable);
    EspaceResponse consulter(Long id);
    EspaceResponse creer(EspaceCreateRequest request);
    EspaceResponse modifier(Long id, EspaceUpdateRequest request);
    boolean existe(Long id);
    StatutEspace statutDe(Long id);
    boolean estArchive(Long id);
    boolean estReservable(Long id);
}

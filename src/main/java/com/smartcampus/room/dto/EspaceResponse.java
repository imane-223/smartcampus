package com.smartcampus.room.dto;

import com.smartcampus.room.model.Espace;
import com.smartcampus.room.model.StatutEspace;
import com.smartcampus.room.model.TypeEspace;

public record EspaceResponse(Long id, String code, String nom, TypeEspace type, int capacite,
        BatimentResponse batiment, Integer etage, String description, StatutEspace statut,
        boolean archive, boolean reservable) {
    public static EspaceResponse from(Espace espace) {
        return new EspaceResponse(espace.getId(), espace.getCode(), espace.getNom(), espace.getType(),
                espace.getCapacite(), BatimentResponse.from(espace.getBatiment()), espace.getEtage(),
                espace.getDescription(), espace.getStatut(), espace.isArchive(), espace.isReservable());
    }
}

package com.smartcampus.room.dto;

import com.smartcampus.room.model.Batiment;

public record BatimentResponse(Long id, String nom) {
    public static BatimentResponse from(Batiment batiment) {
        return new BatimentResponse(batiment.getId(), batiment.getNom());
    }
}

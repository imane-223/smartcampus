package com.smartcampus.room.service;

import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import com.smartcampus.room.model.Espace;
import com.smartcampus.room.model.StatutEspace;
import com.smartcampus.room.model.TypeEspace;

final class EspaceSpecifications {
    private EspaceSpecifications() {
    }

    static Specification<Espace> catalog(TypeEspace type, Long batimentId, StatutEspace statut, String search) {
        return (root, query, cb) -> {
            var predicates = cb.conjunction();
            predicates = cb.and(predicates, cb.isFalse(root.get("archive")));
            if (type != null) {
                predicates = cb.and(predicates, cb.equal(root.get("type"), type));
            }
            if (batimentId != null) {
                predicates = cb.and(predicates, cb.equal(root.get("batiment").get("id"), batimentId));
            }
            if (statut != null) {
                predicates = cb.and(predicates, cb.equal(root.get("statut"), statut));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates = cb.and(predicates, cb.or(
                        cb.like(cb.lower(root.get("nom")), pattern),
                        cb.like(cb.lower(root.get("code")), pattern)));
            }
            return predicates;
        };
    }
}

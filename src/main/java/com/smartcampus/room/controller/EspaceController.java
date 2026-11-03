package com.smartcampus.room.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartcampus.room.dto.EspaceCreateRequest;
import com.smartcampus.room.dto.EspaceResponse;
import com.smartcampus.room.dto.EspaceUpdateRequest;
import com.smartcampus.room.model.StatutEspace;
import com.smartcampus.room.model.TypeEspace;
import com.smartcampus.room.service.EspaceService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/salles")
public class EspaceController {

    private final EspaceService espaceService;

    public EspaceController(EspaceService espaceService) {
        this.espaceService = espaceService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Page<EspaceResponse> rechercher(
            @RequestParam(required = false) TypeEspace type,
            @RequestParam(required = false) Long batimentId,
            @RequestParam(required = false) StatutEspace statut,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "nom", direction = Sort.Direction.ASC) Pageable pageable) {
        return espaceService.rechercher(type, batimentId, statut, search, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public EspaceResponse consulter(@PathVariable Long id) {
        return espaceService.consulter(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EspaceResponse> creer(@Valid @RequestBody EspaceCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(espaceService.creer(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public EspaceResponse modifier(@PathVariable Long id,
            @Valid @RequestBody EspaceUpdateRequest request) {
        return espaceService.modifier(id, request);
    }
}

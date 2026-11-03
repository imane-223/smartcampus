package com.smartcampus.room.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartcampus.room.dto.BatimentResponse;
import com.smartcampus.room.dto.BatimentCreateRequest;
import com.smartcampus.room.service.BatimentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/salles/batiments")
public class BatimentController {
    private final BatimentService service;

    public BatimentController(BatimentService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<BatimentResponse> lister() {
        return service.lister();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BatimentResponse> creer(@Valid @RequestBody BatimentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.creer(request));
    }
}

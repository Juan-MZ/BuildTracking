package com.construmedicis.buildtracking.participation.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.construmedicis.buildtracking.participation.dto.ParticipationDTO;
import com.construmedicis.buildtracking.participation.services.ParticipationService;
import com.construmedicis.buildtracking.util.response.Response;

@RestController
@RequestMapping("/api/participations")
public class ParticipationController {

    private final ParticipationService service;

    public ParticipationController(ParticipationService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Response<List<ParticipationDTO>>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Response<ParticipationDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<Response<ParticipationDTO>> create(@RequestBody ParticipationDTO dto) {
        return ResponseEntity.status(201).body(service.save(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Response<Void>> delete(@PathVariable Long id) {
        return ResponseEntity.ok(service.deleteById(id));
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<Response<List<ParticipationDTO>>> getByProjectId(@PathVariable Long projectId) {
        return ResponseEntity.ok(service.findByProjectId(projectId));
    }
}

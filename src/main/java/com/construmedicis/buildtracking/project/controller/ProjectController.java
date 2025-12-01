package com.construmedicis.buildtracking.project.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.construmedicis.buildtracking.project.dto.ProjectDTO;
import com.construmedicis.buildtracking.project.services.ProjectService;
import com.construmedicis.buildtracking.util.response.Response;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService service;

    public ProjectController(ProjectService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Response<List<ProjectDTO>>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Response<ProjectDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<Response<ProjectDTO>> create(@RequestBody ProjectDTO dto) {
        return ResponseEntity.status(201).body(service.save(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Response<Void>> delete(@PathVariable Long id) {
        return ResponseEntity.ok(service.deleteById(id));
    }
}

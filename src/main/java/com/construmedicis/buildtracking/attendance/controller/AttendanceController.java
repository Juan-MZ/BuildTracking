package com.construmedicis.buildtracking.attendance.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.construmedicis.buildtracking.attendance.dto.AttendanceDTO;
import com.construmedicis.buildtracking.attendance.services.AttendanceService;
import com.construmedicis.buildtracking.util.response.Response;

@RestController
@RequestMapping("/api/attendances")
public class AttendanceController {

    private final AttendanceService service;

    public AttendanceController(AttendanceService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Response<List<AttendanceDTO>>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Response<AttendanceDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<Response<AttendanceDTO>> create(@RequestBody AttendanceDTO dto) {
        return ResponseEntity.status(201).body(service.save(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Response<Void>> delete(@PathVariable Long id) {
        return ResponseEntity.ok(service.deleteById(id));
    }

    @GetMapping("/participation/{participationId}")
    public ResponseEntity<Response<List<AttendanceDTO>>> getByParticipationId(@PathVariable Long participationId) {
        return ResponseEntity.ok(service.findByParticipationId(participationId));
    }

}

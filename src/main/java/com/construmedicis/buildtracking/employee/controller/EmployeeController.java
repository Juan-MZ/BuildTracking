package com.construmedicis.buildtracking.employee.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.construmedicis.buildtracking.employee.dto.EmployeeDTO;
import com.construmedicis.buildtracking.employee.services.EmployeeService;
import com.construmedicis.buildtracking.util.response.Response;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService service;

    public EmployeeController(EmployeeService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Response<List<EmployeeDTO>>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Response<EmployeeDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<Response<EmployeeDTO>> create(@RequestBody EmployeeDTO dto) {
        return ResponseEntity.status(201).body(service.save(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Response<Void>> delete(@PathVariable Long id) {
        return ResponseEntity.ok(service.deleteById(id));
    }
}

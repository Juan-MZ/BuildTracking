package com.construmedicis.buildtracking.item.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.construmedicis.buildtracking.item.dto.ItemDTO;
import com.construmedicis.buildtracking.item.services.ItemService;
import com.construmedicis.buildtracking.util.response.Response;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService service;

    public ItemController(ItemService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Response<List<ItemDTO>>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Response<ItemDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<Response<ItemDTO>> create(@RequestBody ItemDTO dto) {
        return ResponseEntity.status(201).body(service.save(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Response<ItemDTO>> update(@PathVariable Long id, @RequestBody ItemDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<Response<List<ItemDTO>>> getByProjectId(@PathVariable Long projectId) {
        return ResponseEntity.ok(service.findByProjectId(projectId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Response<Void>> delete(@PathVariable Long id) {
        return ResponseEntity.ok(service.deleteById(id));
    }
}

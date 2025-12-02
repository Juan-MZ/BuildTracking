package com.construmedicis.buildtracking.retention.controller;

import com.construmedicis.buildtracking.retention.dto.RetentionConfigDTO;
import com.construmedicis.buildtracking.retention.services.RetentionConfigService;
import com.construmedicis.buildtracking.util.response.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/retention-config")
public class RetentionConfigController {

    private final RetentionConfigService retentionConfigService;

    public RetentionConfigController(RetentionConfigService retentionConfigService) {
        this.retentionConfigService = retentionConfigService;
    }

    @GetMapping
    public ResponseEntity<Response<List<RetentionConfigDTO>>> findAll() {
        Response<List<RetentionConfigDTO>> response = retentionConfigService.findAll();
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("/year/{year}")
    public ResponseEntity<Response<RetentionConfigDTO>> findByYear(@PathVariable Integer year) {
        Response<RetentionConfigDTO> response = retentionConfigService.findByYear(year);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PostMapping
    public ResponseEntity<Response<RetentionConfigDTO>> save(@RequestBody RetentionConfigDTO retentionConfigDTO) {
        Response<RetentionConfigDTO> response = retentionConfigService.save(retentionConfigDTO);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Response<Void>> delete(@PathVariable Long id) {
        Response<Void> response = retentionConfigService.deleteById(id);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}

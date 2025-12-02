package com.construmedicis.buildtracking.email.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.construmedicis.buildtracking.email.dto.EmailConfigDTO;
import com.construmedicis.buildtracking.email.dto.EmailSyncResultDTO;
import com.construmedicis.buildtracking.email.services.EmailConfigService;
import com.construmedicis.buildtracking.util.response.Response;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/email-config")
@RequiredArgsConstructor
public class EmailConfigController {

    private final EmailConfigService emailConfigService;

    @PostMapping
    public ResponseEntity<Response<EmailConfigDTO>> create(@RequestBody EmailConfigDTO emailConfigDTO) {
        EmailConfigDTO created = emailConfigService.create(emailConfigDTO);
        Response<EmailConfigDTO> response = new Response<>();
        response.setStatus(201);
        response.setUserMessage("Email configuration created successfully");
        response.setData(created);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Response<EmailConfigDTO>> update(
            @PathVariable Long id,
            @RequestBody EmailConfigDTO emailConfigDTO) {
        EmailConfigDTO updated = emailConfigService.update(id, emailConfigDTO);
        Response<EmailConfigDTO> response = new Response<>();
        response.setStatus(200);
        response.setUserMessage("Email configuration updated successfully");
        response.setData(updated);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Response<Void>> delete(@PathVariable Long id) {
        emailConfigService.delete(id);
        Response<Void> response = new Response<>();
        response.setStatus(200);
        response.setUserMessage("Email configuration deleted successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Response<EmailConfigDTO>> findById(@PathVariable Long id) {
        EmailConfigDTO emailConfig = emailConfigService.findById(id);
        Response<EmailConfigDTO> response = new Response<>();
        response.setStatus(200);
        response.setUserMessage("Email configuration retrieved successfully");
        response.setData(emailConfig);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Response<List<EmailConfigDTO>>> findAll() {
        List<EmailConfigDTO> emailConfigs = emailConfigService.findAll();
        Response<List<EmailConfigDTO>> response = new Response<>();
        response.setStatus(200);
        response.setUserMessage("Email configurations retrieved successfully");
        response.setData(emailConfigs);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<Response<EmailConfigDTO>> findByProjectId(@PathVariable Long projectId) {
        EmailConfigDTO emailConfig = emailConfigService.findByProjectId(projectId);
        Response<EmailConfigDTO> response = new Response<>();
        response.setStatus(200);
        response.setUserMessage("Email configuration for project retrieved successfully");
        response.setData(emailConfig);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/auto-sync")
    public ResponseEntity<Response<List<EmailConfigDTO>>> findAutoSyncEnabled() {
        List<EmailConfigDTO> emailConfigs = emailConfigService.findAutoSyncEnabled();
        Response<List<EmailConfigDTO>> response = new Response<>();
        response.setStatus(200);
        response.setUserMessage("Auto-sync enabled configurations retrieved successfully");
        response.setData(emailConfigs);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint principal para sincronización manual de facturas desde Gmail.
     * Descarga adjuntos XML, parsea facturas, crea registros en BD,
     * evalúa reglas de asignación y elimina archivos temporales.
     */
    @PostMapping("/{id}/sync")
    public ResponseEntity<Response<EmailSyncResultDTO>> syncEmails(@PathVariable Long id) {
        EmailSyncResultDTO result = emailConfigService.syncEmails(id);
        
        String message = String.format(
                "Sync completed: %s. Processed %d emails, created %d invoices (%d auto-assigned, %d pending review)",
                result.getSyncStatus(),
                result.getEmailsProcessed(),
                result.getInvoicesCreated(),
                result.getInvoicesAutoAssigned(),
                result.getInvoicesPendingReview()
        );
        
        Response<EmailSyncResultDTO> response = new Response<>();
        response.setStatus("SUCCESS".equals(result.getSyncStatus()) ? 200 : 206);
        response.setUserMessage(message);
        response.setData(result);
        
        return ResponseEntity.ok(response);
    }
}

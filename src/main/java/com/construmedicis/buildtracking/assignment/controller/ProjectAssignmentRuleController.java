package com.construmedicis.buildtracking.assignment.controller;

import com.construmedicis.buildtracking.assignment.dto.ProjectAssignmentResultDTO;
import com.construmedicis.buildtracking.assignment.dto.ProjectAssignmentRuleDTO;
import com.construmedicis.buildtracking.assignment.models.ProjectAssignmentRule.RuleType;
import com.construmedicis.buildtracking.assignment.services.ProjectAssignmentRuleService;
import com.construmedicis.buildtracking.invoice.dto.InvoiceDTO;
import com.construmedicis.buildtracking.util.response.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assignment-rules")
public class ProjectAssignmentRuleController {

    private final ProjectAssignmentRuleService service;

    public ProjectAssignmentRuleController(ProjectAssignmentRuleService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Response<List<ProjectAssignmentRuleDTO>>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Response<ProjectAssignmentRuleDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<Response<ProjectAssignmentRuleDTO>> create(@RequestBody ProjectAssignmentRuleDTO ruleDTO) {
        return ResponseEntity.ok(service.save(ruleDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Response<Void>> delete(@PathVariable Long id) {
        return ResponseEntity.ok(service.deleteById(id));
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<Response<List<ProjectAssignmentRuleDTO>>> getByProjectId(@PathVariable Long projectId) {
        return ResponseEntity.ok(service.findByProjectId(projectId));
    }

    @GetMapping("/active")
    public ResponseEntity<Response<List<ProjectAssignmentRuleDTO>>> getActiveRules() {
        return ResponseEntity.ok(service.findActiveRules());
    }

    @GetMapping("/type/{ruleType}")
    public ResponseEntity<Response<List<ProjectAssignmentRuleDTO>>> getByRuleType(@PathVariable RuleType ruleType) {
        return ResponseEntity.ok(service.findByRuleType(ruleType));
    }

    @PostMapping("/evaluate")
    public ResponseEntity<Response<ProjectAssignmentResultDTO>> evaluateRulesForInvoice(@RequestBody InvoiceDTO invoiceDTO) {
        return ResponseEntity.ok(service.evaluateRulesForInvoice(invoiceDTO));
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<Response<ProjectAssignmentRuleDTO>> toggleActive(
            @PathVariable Long id,
            @RequestParam Boolean isActive) {
        return ResponseEntity.ok(service.toggleActive(id, isActive));
    }
}

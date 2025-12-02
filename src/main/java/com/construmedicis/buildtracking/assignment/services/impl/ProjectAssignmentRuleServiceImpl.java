package com.construmedicis.buildtracking.assignment.services.impl;

import com.construmedicis.buildtracking.assignment.dto.ProjectAssignmentResultDTO;
import com.construmedicis.buildtracking.assignment.dto.ProjectAssignmentRuleDTO;
import com.construmedicis.buildtracking.assignment.models.ProjectAssignmentRule;
import com.construmedicis.buildtracking.assignment.models.ProjectAssignmentRule.RuleType;
import com.construmedicis.buildtracking.assignment.repository.ProjectAssignmentRuleRepository;
import com.construmedicis.buildtracking.assignment.services.ProjectAssignmentRuleService;
import com.construmedicis.buildtracking.invoice.dto.InvoiceDTO;
import com.construmedicis.buildtracking.invoice.repository.InvoiceItemRepository;
import com.construmedicis.buildtracking.participation.repository.ParticipationRepository;
import com.construmedicis.buildtracking.project.repository.ProjectRepository;
import com.construmedicis.buildtracking.util.exception.BusinessRuleException;
import com.construmedicis.buildtracking.util.response.Response;
import com.construmedicis.buildtracking.util.response.handler.ResponseHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProjectAssignmentRuleServiceImpl implements ProjectAssignmentRuleService {

    private final ProjectAssignmentRuleRepository repository;
    private final ProjectRepository projectRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final ParticipationRepository participationRepository;

    public ProjectAssignmentRuleServiceImpl(ProjectAssignmentRuleRepository repository,
                                           ProjectRepository projectRepository,
                                           InvoiceItemRepository invoiceItemRepository,
                                           ParticipationRepository participationRepository) {
        this.repository = repository;
        this.projectRepository = projectRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.participationRepository = participationRepository;
    }

    @Override
    @Transactional
    public Response<ProjectAssignmentRuleDTO> save(ProjectAssignmentRuleDTO ruleDTO) {
        if (!projectRepository.existsById(ruleDTO.getProjectId())) {
            throw new BusinessRuleException("project.not.found");
        }

        ProjectAssignmentRule rule = fromDTO(ruleDTO);
        ProjectAssignmentRule saved = repository.save(rule);
        return new ResponseHandler<>(201, "Assignment rule created", "/api/assignment-rules", toDTO(saved)).getResponse();
    }

    @Override
    public Response<ProjectAssignmentRuleDTO> findById(Long id) {
        ProjectAssignmentRule rule = repository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("assignment.rule.not.found"));
        return new ResponseHandler<>(200, "Assignment rule found", "/api/assignment-rules/{id}", toDTO(rule)).getResponse();
    }

    @Override
    public Response<List<ProjectAssignmentRuleDTO>> findAll() {
        List<ProjectAssignmentRuleDTO> rules = repository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return new ResponseHandler<>(200, "Assignment rules found", "/api/assignment-rules", rules).getResponse();
    }

    @Override
    @Transactional
    public Response<Void> deleteById(Long id) {
        if (!repository.existsById(id)) {
            throw new BusinessRuleException("assignment.rule.not.found");
        }
        repository.deleteById(id);
        return new ResponseHandler<Void>(200, "Assignment rule deleted", "/api/assignment-rules/{id}", null).getResponse();
    }

    @Override
    public Response<List<ProjectAssignmentRuleDTO>> findByProjectId(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new BusinessRuleException("project.not.found");
        }
        List<ProjectAssignmentRuleDTO> rules = repository.findByProjectId(projectId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return new ResponseHandler<>(200, "Assignment rules found", "/api/assignment-rules/project/{projectId}", rules).getResponse();
    }

    @Override
    public Response<List<ProjectAssignmentRuleDTO>> findActiveRules() {
        List<ProjectAssignmentRuleDTO> rules = repository.findByIsActiveTrueOrderByPriorityAsc().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return new ResponseHandler<>(200, "Active rules found", "/api/assignment-rules/active", rules).getResponse();
    }

    @Override
    public Response<List<ProjectAssignmentRuleDTO>> findByRuleType(RuleType ruleType) {
        List<ProjectAssignmentRuleDTO> rules = repository.findByRuleTypeAndIsActiveTrue(ruleType).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return new ResponseHandler<>(200, "Rules found by type", "/api/assignment-rules/type/{ruleType}", rules).getResponse();
    }

    @Override
    public Response<ProjectAssignmentResultDTO> evaluateRulesForInvoice(InvoiceDTO invoiceDTO) {
        List<ProjectAssignmentRule> activeRules = repository.findByIsActiveTrueOrderByPriorityAsc();

        for (ProjectAssignmentRule rule : activeRules) {
            ProjectAssignmentResultDTO result = evaluateSingleRule(rule, invoiceDTO);
            if (result != null && result.getConfidence() > 0) {
                return new ResponseHandler<>(200, "Project assignment evaluated", "/api/assignment-rules/evaluate", result).getResponse();
            }
        }

        // No se encontró ninguna regla que coincida
        ProjectAssignmentResultDTO noMatch = ProjectAssignmentResultDTO.builder()
                .projectId(null)
                .projectName(null)
                .confidence(0)
                .matchedRuleType("NONE")
                .matchReason("No se encontró ninguna regla que coincida con esta factura")
                .build();

        return new ResponseHandler<>(200, "No matching rule found", "/api/assignment-rules/evaluate", noMatch).getResponse();
    }

    @Override
    @Transactional
    public Response<ProjectAssignmentRuleDTO> toggleActive(Long id, Boolean isActive) {
        ProjectAssignmentRule rule = repository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("assignment.rule.not.found"));
        
        rule.setIsActive(isActive);
        ProjectAssignmentRule updated = repository.save(rule);
        return new ResponseHandler<>(200, "Rule status updated", "/api/assignment-rules/{id}/toggle", toDTO(updated)).getResponse();
    }

    private ProjectAssignmentResultDTO evaluateSingleRule(ProjectAssignmentRule rule, InvoiceDTO invoice) {
        boolean matches = false;
        int confidence = 0;
        String matchReason = "";

        switch (rule.getRuleType()) {
            case SUPPLIER_NIT:
                if (rule.getSupplierNit() != null && rule.getSupplierNit().equals(invoice.getSupplierId())) {
                    matches = true;
                    confidence = 95; // Alta confianza por NIT
                    matchReason = "NIT del proveedor coincide: " + invoice.getSupplierId();
                }
                break;

            case DATE_RANGE:
                if (rule.getStartDate() != null && rule.getEndDate() != null && invoice.getIssueDate() != null) {
                    LocalDate issueDate = invoice.getIssueDate();
                    if (!issueDate.isBefore(rule.getStartDate()) && !issueDate.isAfter(rule.getEndDate())) {
                        matches = true;
                        confidence = 70; // Media confianza por fecha
                        matchReason = "Fecha de factura dentro del rango del proyecto";
                    }
                }
                break;

            case KEYWORDS:
                if (rule.getKeywords() != null && invoice.getId() != null) {
                    // Parse keywords manualmente (formato simple separado por comas)
                    List<String> keywords = Arrays.asList(rule.getKeywords().split(","))
                            .stream()
                            .map(String::trim)
                            .collect(Collectors.toList());
                    
                    long matchCount = invoiceItemRepository.findByInvoiceId(invoice.getId()).stream()
                            .filter(item -> {
                                String desc = item.getDescription().toLowerCase();
                                return keywords.stream().anyMatch(kw -> desc.contains(kw.toLowerCase()));
                            })
                            .count();

                    if (matchCount > 0) {
                        matches = true;
                        confidence = Math.min(60 + (int)(matchCount * 10), 85); // 60-85% según matches
                        matchReason = "Encontradas " + matchCount + " palabra(s) clave en las líneas de factura";
                    }
                }
                break;

            case EMPLOYEE_PARTICIPATION:
                if (invoice.getIssueDate() != null) {
                    // Verificar si hay empleados participando en el proyecto
                    long participationCount = participationRepository.findByProjectId(rule.getProject().getId()).size();

                    if (participationCount > 0) {
                        matches = true;
                        confidence = 75; // Buena confianza por participación activa
                        matchReason = "Hay " + participationCount + " participación(es) en el proyecto";
                    }
                }
                break;

            case MANUAL:
                // Las reglas manuales siempre requieren confirmación
                confidence = 0;
                break;
        }

        if (matches) {
            return ProjectAssignmentResultDTO.builder()
                    .projectId(rule.getProject().getId())
                    .projectName(rule.getProject().getName())
                    .confidence(confidence)
                    .matchedRuleType(rule.getRuleType().name())
                    .matchReason(matchReason)
                    .build();
        }

        return null;
    }

    private ProjectAssignmentRuleDTO toDTO(ProjectAssignmentRule rule) {
        if (rule == null) return null;
        return ProjectAssignmentRuleDTO.builder()
                .id(rule.getId())
                .projectId(rule.getProject() != null ? rule.getProject().getId() : null)
                .priority(rule.getPriority())
                .ruleType(rule.getRuleType())
                .supplierNit(rule.getSupplierNit())
                .startDate(rule.getStartDate())
                .endDate(rule.getEndDate())
                .keywords(rule.getKeywords())
                .isActive(rule.getIsActive())
                .description(rule.getDescription())
                .build();
    }

    private ProjectAssignmentRule fromDTO(ProjectAssignmentRuleDTO dto) {
        ProjectAssignmentRule rule = ProjectAssignmentRule.builder()
                .id(dto.getId())
                .priority(dto.getPriority())
                .ruleType(dto.getRuleType())
                .supplierNit(dto.getSupplierNit())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .keywords(dto.getKeywords())
                .isActive(dto.getIsActive())
                .description(dto.getDescription())
                .build();

        if (dto.getProjectId() != null) {
            rule.setProject(projectRepository.findById(dto.getProjectId()).orElse(null));
        }

        return rule;
    }
}

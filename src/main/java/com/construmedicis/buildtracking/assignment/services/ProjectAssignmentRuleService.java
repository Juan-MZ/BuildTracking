package com.construmedicis.buildtracking.assignment.services;

import com.construmedicis.buildtracking.assignment.dto.ProjectAssignmentResultDTO;
import com.construmedicis.buildtracking.assignment.dto.ProjectAssignmentRuleDTO;
import com.construmedicis.buildtracking.assignment.models.ProjectAssignmentRule.RuleType;
import com.construmedicis.buildtracking.invoice.dto.InvoiceDTO;
import com.construmedicis.buildtracking.util.response.Response;

import java.util.List;

public interface ProjectAssignmentRuleService {

    Response<ProjectAssignmentRuleDTO> save(ProjectAssignmentRuleDTO ruleDTO);

    Response<ProjectAssignmentRuleDTO> findById(Long id);

    Response<List<ProjectAssignmentRuleDTO>> findAll();

    Response<Void> deleteById(Long id);

    Response<List<ProjectAssignmentRuleDTO>> findByProjectId(Long projectId);

    Response<List<ProjectAssignmentRuleDTO>> findActiveRules();

    Response<List<ProjectAssignmentRuleDTO>> findByRuleType(RuleType ruleType);

    Response<ProjectAssignmentResultDTO> evaluateRulesForInvoice(InvoiceDTO invoiceDTO);

    Response<ProjectAssignmentRuleDTO> toggleActive(Long id, Boolean isActive);
}

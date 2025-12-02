package com.construmedicis.buildtracking.assignment.dto;

import com.construmedicis.buildtracking.assignment.models.ProjectAssignmentRule.RuleType;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectAssignmentRuleDTO {

    private Long id;
    private Long projectId;
    private Integer priority;
    private RuleType ruleType;
    private String supplierNit;
    private LocalDate startDate;
    private LocalDate endDate;
    private String keywords; // JSON string
    private Boolean isActive;
    private String description;
}

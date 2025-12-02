package com.construmedicis.buildtracking.assignment.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectAssignmentResultDTO {

    private Long projectId;
    private String projectName;
    private Integer confidence; // 0-100%
    private String matchedRuleType;
    private String matchReason; // Explicación de por qué se asignó
}

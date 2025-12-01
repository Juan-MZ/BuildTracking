package com.construmedicis.buildtracking.participation.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ParticipationDTO {
    private Long id;
    private String role;
    private Long employeeId;
    private Long projectId;
}

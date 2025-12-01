package com.construmedicis.buildtracking.employee.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmployeeDTO {
    private Long id;
    private String identificationType;
    private String identificationNumber;
    private LocalDate birthDate;
    private String firstName;
    private String lastName;
    private String position;
    private String phoneNumber;
    private List<Long> projectIds;
    private Long managerId;
    private List<Long> subordinateIds;
}

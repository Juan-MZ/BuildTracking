package com.construmedicis.buildtracking.email.dto;

import java.time.LocalDateTime;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmailConfigDTO {

    private Long id;
    private Long projectId;
    private String credentialsPath;
    private String tokensDirectory;
    private String gmailLabel;
    private LocalDateTime lastSyncDate;
    private Boolean autoSyncEnabled;
    private Integer syncFrequencyHours;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
}

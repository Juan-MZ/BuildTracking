package com.construmedicis.buildtracking.email.dto;

import java.util.List;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmailSyncResultDTO {

    private Integer emailsProcessed;
    private Integer invoicesCreated;
    private Integer invoicesAutoAssigned;
    private Integer invoicesPendingReview;
    private List<String> errors;
    private String syncStatus; // SUCCESS, PARTIAL_SUCCESS, FAILED
}

package com.construmedicis.buildtracking.retention.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetentionConfigDTO {

    private Long id;
    private Integer year;
    private BigDecimal minAmountICA;
    private BigDecimal minAmountFuente;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

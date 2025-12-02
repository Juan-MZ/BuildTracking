package com.construmedicis.buildtracking.retention.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "retention_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetentionConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "retention_config_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private Integer year;

    @Column(name = "minimum_amount_ica", nullable = false, precision = 15, scale = 2)
    private BigDecimal minAmountICA;

    @Column(name = "minimum_amount_fuente", nullable = false, precision = 15, scale = 2)
    private BigDecimal minAmountFuente;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

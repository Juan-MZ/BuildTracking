package com.construmedicis.buildtracking.email.models;

import java.time.LocalDateTime;

import com.construmedicis.buildtracking.project.models.Project;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "email_config")
public class EmailConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "email_config_id_seq")
    @SequenceGenerator(name = "email_config_id_seq", sequenceName = "email_config_id_seq", allocationSize = 1)
    @Column(name = "email_config_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "credentials_path", nullable = false)
    private String credentialsPath;

    @Column(name = "tokens_directory", nullable = false)
    private String tokensDirectory;

    @Column(name = "gmail_label", nullable = false)
    private String gmailLabel;

    @Column(name = "last_sync_date")
    private LocalDateTime lastSyncDate;

    @Column(name = "auto_sync_enabled", nullable = false)
    private Boolean autoSyncEnabled;

    @Column(name = "sync_frequency_hours")
    private Integer syncFrequencyHours; // Para el scheduler opcional

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        updatedDate = LocalDateTime.now();
        if (autoSyncEnabled == null) {
            autoSyncEnabled = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDateTime.now();
    }
}

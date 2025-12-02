package com.construmedicis.buildtracking.email.services.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.construmedicis.buildtracking.email.dto.EmailConfigDTO;
import com.construmedicis.buildtracking.email.dto.EmailSyncResultDTO;
import com.construmedicis.buildtracking.email.models.EmailConfig;
import com.construmedicis.buildtracking.email.repository.EmailConfigRepository;
import com.construmedicis.buildtracking.email.services.EmailConfigService;
import com.construmedicis.buildtracking.email.services.EmailExtractionService;
import com.construmedicis.buildtracking.project.models.Project;
import com.construmedicis.buildtracking.project.repository.ProjectRepository;
import com.construmedicis.buildtracking.util.exception.BusinessRuleException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailConfigServiceImpl implements EmailConfigService {

    private final EmailConfigRepository emailConfigRepository;
    private final ProjectRepository projectRepository;
    private final EmailExtractionService emailExtractionService;

    @Override
    @Transactional
    public EmailConfigDTO create(EmailConfigDTO emailConfigDTO) {
        Project project = projectRepository.findById(emailConfigDTO.getProjectId())
                .orElseThrow(() -> new BusinessRuleException("project.not.found"));

        // Verificar que el proyecto no tenga ya una configuración
        emailConfigRepository.findByProjectId(project.getId())
                .ifPresent(existing -> {
                    throw new BusinessRuleException("email.config.already.exists");
                });

        EmailConfig emailConfig = EmailConfig.builder()
                .project(project)
                .credentialsPath(emailConfigDTO.getCredentialsPath())
                .tokensDirectory(emailConfigDTO.getTokensDirectory())
                .gmailLabel(emailConfigDTO.getGmailLabel())
                .lastSyncDate(emailConfigDTO.getLastSyncDate())
                .autoSyncEnabled(emailConfigDTO.getAutoSyncEnabled())
                .syncFrequencyHours(emailConfigDTO.getSyncFrequencyHours())
                .build();

        EmailConfig saved = emailConfigRepository.save(emailConfig);
        return toDTO(saved);
    }

    @Override
    @Transactional
    public EmailConfigDTO update(Long id, EmailConfigDTO emailConfigDTO) {
        EmailConfig emailConfig = emailConfigRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("email.config.not.found"));

        if (emailConfigDTO.getProjectId() != null &&
                !emailConfig.getProject().getId().equals(emailConfigDTO.getProjectId())) {
            Project project = projectRepository.findById(emailConfigDTO.getProjectId())
                    .orElseThrow(() -> new BusinessRuleException("project.not.found"));
            emailConfig.setProject(project);
        }

        if (emailConfigDTO.getCredentialsPath() != null) {
            emailConfig.setCredentialsPath(emailConfigDTO.getCredentialsPath());
        }
        if (emailConfigDTO.getTokensDirectory() != null) {
            emailConfig.setTokensDirectory(emailConfigDTO.getTokensDirectory());
        }
        if (emailConfigDTO.getGmailLabel() != null) {
            emailConfig.setGmailLabel(emailConfigDTO.getGmailLabel());
        }
        if (emailConfigDTO.getAutoSyncEnabled() != null) {
            emailConfig.setAutoSyncEnabled(emailConfigDTO.getAutoSyncEnabled());
        }
        if (emailConfigDTO.getSyncFrequencyHours() != null) {
            emailConfig.setSyncFrequencyHours(emailConfigDTO.getSyncFrequencyHours());
        }

        EmailConfig updated = emailConfigRepository.save(emailConfig);
        return toDTO(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        EmailConfig emailConfig = emailConfigRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("email.config.not.found"));
        emailConfigRepository.delete(emailConfig);
    }

    @Override
    @Transactional(readOnly = true)
    public EmailConfigDTO findById(Long id) {
        EmailConfig emailConfig = emailConfigRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("email.config.not.found"));
        return toDTO(emailConfig);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmailConfigDTO> findAll() {
        return emailConfigRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public EmailConfigDTO findByProjectId(Long projectId) {
        EmailConfig emailConfig = emailConfigRepository.findByProjectId(projectId)
                .orElseThrow(() -> new BusinessRuleException("email.config.not.found"));
        return toDTO(emailConfig);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmailConfigDTO> findAutoSyncEnabled() {
        return emailConfigRepository.findByAutoSyncEnabledTrue().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EmailSyncResultDTO syncEmails(Long id) {
        EmailConfig emailConfig = emailConfigRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("email.config.not.found"));

        // Delegar la sincronización al EmailExtractionService
        EmailSyncResultDTO result = emailExtractionService.syncEmailsForConfig(emailConfig);

        // Actualizar lastSyncDate si la sincronización fue exitosa
        if ("SUCCESS".equals(result.getSyncStatus()) || "PARTIAL_SUCCESS".equals(result.getSyncStatus())) {
            emailConfig.setLastSyncDate(java.time.LocalDateTime.now());
            emailConfigRepository.save(emailConfig);
        }

        return result;
    }

    private EmailConfigDTO toDTO(EmailConfig emailConfig) {
        return EmailConfigDTO.builder()
                .id(emailConfig.getId())
                .projectId(emailConfig.getProject().getId())
                .credentialsPath(emailConfig.getCredentialsPath())
                .tokensDirectory(emailConfig.getTokensDirectory())
                .gmailLabel(emailConfig.getGmailLabel())
                .lastSyncDate(emailConfig.getLastSyncDate())
                .autoSyncEnabled(emailConfig.getAutoSyncEnabled())
                .syncFrequencyHours(emailConfig.getSyncFrequencyHours())
                .createdDate(emailConfig.getCreatedDate())
                .updatedDate(emailConfig.getUpdatedDate())
                .build();
    }
}

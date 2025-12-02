package com.construmedicis.buildtracking.email.services;

import java.util.List;

import com.construmedicis.buildtracking.email.dto.EmailConfigDTO;
import com.construmedicis.buildtracking.email.dto.EmailSyncResultDTO;

public interface EmailConfigService {

    EmailConfigDTO create(EmailConfigDTO emailConfigDTO);
    
    EmailConfigDTO update(Long id, EmailConfigDTO emailConfigDTO);
    
    void delete(Long id);
    
    EmailConfigDTO findById(Long id);
    
    List<EmailConfigDTO> findAll();
    
    EmailConfigDTO findByProjectId(Long projectId);
    
    List<EmailConfigDTO> findAutoSyncEnabled();
    
    EmailSyncResultDTO syncEmails(Long id);
}

package com.construmedicis.buildtracking.email.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.construmedicis.buildtracking.email.models.EmailConfig;

@Repository
public interface EmailConfigRepository extends JpaRepository<EmailConfig, Long> {

    Optional<EmailConfig> findByProjectId(Long projectId);

    List<EmailConfig> findByAutoSyncEnabledTrue();
}

package com.construmedicis.buildtracking.retention.repository;

import com.construmedicis.buildtracking.retention.models.RetentionConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RetentionConfigRepository extends JpaRepository<RetentionConfig, Long> {

    Optional<RetentionConfig> findByYear(Integer year);

    boolean existsByYear(Integer year);
}

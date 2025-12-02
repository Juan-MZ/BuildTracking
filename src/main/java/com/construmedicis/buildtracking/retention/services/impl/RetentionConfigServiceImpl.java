package com.construmedicis.buildtracking.retention.services.impl;

import com.construmedicis.buildtracking.retention.dto.RetentionConfigDTO;
import com.construmedicis.buildtracking.retention.models.RetentionConfig;
import com.construmedicis.buildtracking.retention.repository.RetentionConfigRepository;
import com.construmedicis.buildtracking.retention.services.RetentionConfigService;
import com.construmedicis.buildtracking.util.exception.BusinessRuleException;
import com.construmedicis.buildtracking.util.response.Response;
import com.construmedicis.buildtracking.util.response.handler.ResponseHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RetentionConfigServiceImpl implements RetentionConfigService {

    private final RetentionConfigRepository repository;

    public RetentionConfigServiceImpl(RetentionConfigRepository repository) {
        this.repository = repository;
    }

    @Override
    public Response<List<RetentionConfigDTO>> findAll() {
        List<RetentionConfigDTO> configs = repository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return new ResponseHandler<>(200, "Retention configurations found", "/api/retention-config", configs)
                .getResponse();
    }

    @Override
    public Response<RetentionConfigDTO> findByYear(Integer year) {
        RetentionConfig config = repository.findByYear(year)
                .orElseThrow(() -> new BusinessRuleException("retention.config.not.found.for.year"));
        return new ResponseHandler<>(200, "Retention configuration found", "/api/retention-config/year/{year}",
                toDTO(config)).getResponse();
    }

    @Override
    @Transactional
    public Response<RetentionConfigDTO> save(RetentionConfigDTO dto) {
        if (dto.getYear() == null) {
            throw new BusinessRuleException("retention.config.year.required");
        }
        if (dto.getMinimumAmount() == null) {
            throw new BusinessRuleException("retention.config.minimum.amount.required");
        }

        RetentionConfig config;
        boolean isUpdate = false;

        // Si se proporciona un ID, actualizar el registro existente
        if (dto.getId() != null) {
            config = repository.findById(dto.getId())
                    .orElseThrow(() -> new BusinessRuleException("retention.config.not.found"));
            isUpdate = true;
        }
        // Si existe una configuración para el año, actualizarla
        else if (repository.existsByYear(dto.getYear())) {
            config = repository.findByYear(dto.getYear())
                    .orElseThrow(() -> new BusinessRuleException("retention.config.not.found"));
            isUpdate = true;
        }
        // Crear nueva configuración
        else {
            config = new RetentionConfig();
        }

        config.setYear(dto.getYear());
        config.setMinimumAmount(dto.getMinimumAmount());

        RetentionConfig saved = repository.save(config);

        int statusCode = isUpdate ? 200 : 201;
        String message = isUpdate ? "Retention configuration updated" : "Retention configuration created";

        return new ResponseHandler<>(statusCode, message, "/api/retention-config", toDTO(saved))
                .getResponse();
    }

    @Override
    @Transactional
    public Response<Void> deleteById(Long id) {
        if (!repository.existsById(id)) {
            throw new BusinessRuleException("retention.config.not.found");
        }
        repository.deleteById(id);
        return new ResponseHandler<Void>(200, "Retention configuration deleted", "/api/retention-config/{id}", null)
                .getResponse();
    }

    private RetentionConfigDTO toDTO(RetentionConfig config) {
        if (config == null)
            return null;
        return RetentionConfigDTO.builder()
                .id(config.getId())
                .year(config.getYear())
                .minimumAmount(config.getMinimumAmount())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}

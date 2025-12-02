package com.construmedicis.buildtracking.retention.services;

import com.construmedicis.buildtracking.retention.dto.RetentionConfigDTO;
import com.construmedicis.buildtracking.util.response.Response;

import java.util.List;

public interface RetentionConfigService {

    Response<List<RetentionConfigDTO>> findAll();

    Response<RetentionConfigDTO> findByYear(Integer year);

    Response<RetentionConfigDTO> save(RetentionConfigDTO retentionConfigDTO);

    Response<Void> deleteById(Long id);
}

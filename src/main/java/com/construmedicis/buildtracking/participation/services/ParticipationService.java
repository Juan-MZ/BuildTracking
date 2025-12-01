package com.construmedicis.buildtracking.participation.services;

import java.util.List;

import com.construmedicis.buildtracking.participation.dto.ParticipationDTO;
import com.construmedicis.buildtracking.util.response.Response;

public interface ParticipationService {
    Response<List<ParticipationDTO>> findAll();

    Response<ParticipationDTO> findById(Long id);

    Response<ParticipationDTO> save(ParticipationDTO participation);

    Response<Void> deleteById(Long id);
}

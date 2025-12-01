package com.construmedicis.buildtracking.project.services;

import java.util.List;

import com.construmedicis.buildtracking.project.dto.ProjectDTO;
import com.construmedicis.buildtracking.util.response.Response;

public interface ProjectService {
    Response<List<ProjectDTO>> findAll();

    Response<ProjectDTO> findById(Long id);

    Response<ProjectDTO> save(ProjectDTO project);

    Response<Void> deleteById(Long id);
}

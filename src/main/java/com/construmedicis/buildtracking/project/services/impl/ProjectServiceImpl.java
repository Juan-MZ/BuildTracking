package com.construmedicis.buildtracking.project.services.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.construmedicis.buildtracking.project.dto.ProjectDTO;
import com.construmedicis.buildtracking.project.models.Project;
import com.construmedicis.buildtracking.project.repository.ProjectRepository;
import com.construmedicis.buildtracking.project.services.ProjectService;
import com.construmedicis.buildtracking.util.exception.BusinessRuleException;
import com.construmedicis.buildtracking.util.response.Response;
import com.construmedicis.buildtracking.util.response.handler.ResponseHandler;

@Service
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository repository;

    public ProjectServiceImpl(ProjectRepository repository) {
        this.repository = repository;
    }

    @Override
    public Response<List<ProjectDTO>> findAll() {
        var list = repository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
        return new ResponseHandler<>(200, "Projects fetched", "/api/projects", list).getResponse();
    }

    @Override
    public Response<ProjectDTO> findById(Long id) {
        Optional<Project> opt = repository.findById(id);
        if (opt.isEmpty())
            throw new BusinessRuleException("project.not.found");
        return new ResponseHandler<>(200, "Project found", "/api/projects/{id}", toDTO(opt.get())).getResponse();
    }

    @Override
    public Response<ProjectDTO> save(ProjectDTO project) {
        Project p = fromDTO(project);
        Project saved = repository.save(p);
        return new ResponseHandler<>(201, "Project saved", "/api/projects", toDTO(saved)).getResponse();
    }

    @Override
    public Response<Void> deleteById(Long id) {
        if (!repository.existsById(id))
            throw new BusinessRuleException("project.not.found");
        repository.deleteById(id);
        return new ResponseHandler<Void>(200, "Project deleted", "/api/projects/{id}", null).getResponse();
    }

    private ProjectDTO toDTO(Project p) {
        if (p == null)
            return null;
        return ProjectDTO.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .status(p.getStatus())
                .clientName(p.getClientName())
                .location(p.getLocation())
                .creationDate(p.getCreationDate())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .build();
    }

    private Project fromDTO(ProjectDTO dto) {
        if (dto == null)
            return null;
        Project p = new Project();
        p.setId(dto.getId());
        p.setName(dto.getName());
        p.setDescription(dto.getDescription());
        p.setStatus(dto.getStatus());
        p.setClientName(dto.getClientName());
        p.setLocation(dto.getLocation());
        p.setCreationDate(dto.getCreationDate());
        p.setStartDate(dto.getStartDate());
        p.setEndDate(dto.getEndDate());
        return p;
    }
}

package com.construmedicis.buildtracking.participation.services.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.construmedicis.buildtracking.employee.repository.EmployeeRepository;
import com.construmedicis.buildtracking.participation.dto.ParticipationDTO;
import com.construmedicis.buildtracking.participation.models.Participation;
import com.construmedicis.buildtracking.participation.repository.ParticipationRepository;
import com.construmedicis.buildtracking.project.repository.ProjectRepository;
import com.construmedicis.buildtracking.participation.services.ParticipationService;
import com.construmedicis.buildtracking.util.exception.BusinessRuleException;
import com.construmedicis.buildtracking.util.response.Response;
import com.construmedicis.buildtracking.util.response.handler.ResponseHandler;

@Service
public class ParticipationServiceImpl implements ParticipationService {

    private final ParticipationRepository repository;
    private final EmployeeRepository employeeRepository;
    private final ProjectRepository projectRepository;

    public ParticipationServiceImpl(ParticipationRepository repository, EmployeeRepository employeeRepository,
            ProjectRepository projectRepository) {
        this.repository = repository;
        this.employeeRepository = employeeRepository;
        this.projectRepository = projectRepository;
    }

    @Override
    public Response<List<ParticipationDTO>> findAll() {
        var list = repository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
        return new ResponseHandler<>(200, "Participations fetched", "/api/participations", list).getResponse();
    }

    @Override
    public Response<ParticipationDTO> findById(Long id) {
        Optional<Participation> opt = repository.findById(id);
        if (opt.isEmpty())
            throw new BusinessRuleException("participation.not.found");
        return new ResponseHandler<>(200, "Participation found", "/api/participations/{id}", toDTO(opt.get()))
                .getResponse();
    }

    @Override
    public Response<ParticipationDTO> save(ParticipationDTO participation) {
        Participation p = fromDTO(participation);
        Participation saved = repository.save(p);
        return new ResponseHandler<>(201, "Participation saved", "/api/participations", toDTO(saved)).getResponse();
    }

    @Override
    public Response<Void> deleteById(Long id) {
        if (!repository.existsById(id))
            throw new BusinessRuleException("participation.not.found");
        repository.deleteById(id);
        return new ResponseHandler<Void>(200, "Participation deleted", "/api/participations/{id}", null).getResponse();
    }

    private ParticipationDTO toDTO(Participation p) {
        if (p == null)
            return null;
        return ParticipationDTO.builder()
                .id(p.getId())
                .role(p.getRole())
                .employeeId(p.getEmployee() != null ? p.getEmployee().getId() : null)
                .projectId(p.getProject() != null ? p.getProject().getId() : null)
                .build();
    }

    private Participation fromDTO(ParticipationDTO dto) {
        if (dto == null)
            return null;
        Participation p = new Participation();
        p.setId(dto.getId());
        p.setRole(dto.getRole());
        if (dto.getEmployeeId() != null) {
            var e = employeeRepository.findById(dto.getEmployeeId());
            if (e.isEmpty())
                throw new BusinessRuleException("employee.not.found");
            p.setEmployee(e.get());
        }
        if (dto.getProjectId() != null) {
            var pr = projectRepository.findById(dto.getProjectId());
            if (pr.isEmpty())
                throw new BusinessRuleException("project.not.found");
            p.setProject(pr.get());
        }
        return p;
    }
}

package com.construmedicis.buildtracking.attendance.services.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.construmedicis.buildtracking.attendance.dto.AttendanceDTO;
import com.construmedicis.buildtracking.attendance.models.Attendance;
import com.construmedicis.buildtracking.attendance.repository.AttendanceRepository;
import com.construmedicis.buildtracking.attendance.services.AttendanceService;
import com.construmedicis.buildtracking.participation.repository.ParticipationRepository;
import com.construmedicis.buildtracking.util.exception.BusinessRuleException;
import com.construmedicis.buildtracking.util.response.Response;
import com.construmedicis.buildtracking.util.response.handler.ResponseHandler;

@Service
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository repository;
    private final ParticipationRepository participationRepository;

    public AttendanceServiceImpl(AttendanceRepository repository, ParticipationRepository participationRepository) {
        this.repository = repository;
        this.participationRepository = participationRepository;
    }

    @Override
    public Response<List<AttendanceDTO>> findAll() {
        var dtos = repository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
        return new ResponseHandler<>(200, "Attendances fetched", "/api/attendances", dtos).getResponse();
    }

    @Override
    public Response<AttendanceDTO> findById(Long id) {
        Optional<Attendance> opt = repository.findById(id);
        if (opt.isEmpty()) {
            throw new BusinessRuleException("attendance.not.found");
        }
        return new ResponseHandler<>(200, "Attendance found", "/api/attendances/{id}", toDTO(opt.get())).getResponse();
    }

    @Override
    public Response<AttendanceDTO> save(AttendanceDTO attendance) {
        Attendance a = fromDTO(attendance);
        Attendance saved = repository.save(a);
        return new ResponseHandler<>(201, "Attendance saved", "/api/attendances", toDTO(saved)).getResponse();
    }

    @Override
    public Response<Void> deleteById(Long id) {
        if (!repository.existsById(id)) {
            throw new BusinessRuleException("attendance.not.found");
        }
        repository.deleteById(id);
        return new ResponseHandler<Void>(200, "Attendance deleted", "/api/attendances/{id}", null).getResponse();
    }

    @Override
    public Response<List<AttendanceDTO>> findByParticipationId(Long participationId) {
        if (!participationRepository.existsById(participationId))
            throw new BusinessRuleException("participation.not.found");
        var list = repository.findByParticipationId(participationId).stream().map(this::toDTO).collect(Collectors.toList());
        return new ResponseHandler<>(200, "Attendances fetched by participation", "/api/attendances/participation/{participationId}", list).getResponse();
    }

    private AttendanceDTO toDTO(Attendance a) {
        if (a == null)
            return null;
        return AttendanceDTO.builder()
                .id(a.getId())
                .attendanceDate(a.getAttendanceDate())
                .present(a.getPresent())
                .participationId(a.getParticipation() != null ? a.getParticipation().getId() : null)
                .build();
    }

    private Attendance fromDTO(AttendanceDTO dto) {
        if (dto == null)
            return null;
        Attendance a = new Attendance();
        a.setId(dto.getId());
        a.setAttendanceDate(dto.getAttendanceDate());
        a.setPresent(dto.getPresent());
        if (dto.getParticipationId() != null) {
            var pOpt = participationRepository.findById(dto.getParticipationId());
            if (pOpt.isEmpty()) {
                throw new BusinessRuleException("participation.not.found");
            }
            a.setParticipation(pOpt.get());
        }
        return a;
    }
}

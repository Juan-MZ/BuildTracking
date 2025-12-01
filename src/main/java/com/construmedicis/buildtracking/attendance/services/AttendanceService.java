package com.construmedicis.buildtracking.attendance.services;

import java.util.List;

import com.construmedicis.buildtracking.attendance.dto.AttendanceDTO;
import com.construmedicis.buildtracking.util.response.Response;

public interface AttendanceService {
    Response<List<AttendanceDTO>> findAll();

    Response<AttendanceDTO> findById(Long id);

    Response<AttendanceDTO> save(AttendanceDTO attendance);

    Response<Void> deleteById(Long id);
}

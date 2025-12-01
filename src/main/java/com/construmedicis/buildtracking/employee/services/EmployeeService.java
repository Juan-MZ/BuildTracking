package com.construmedicis.buildtracking.employee.services;

import java.util.List;

import com.construmedicis.buildtracking.employee.dto.EmployeeDTO;
import com.construmedicis.buildtracking.util.response.Response;

public interface EmployeeService {
    Response<List<EmployeeDTO>> findAll();

    Response<EmployeeDTO> findById(Long id);

    Response<EmployeeDTO> save(EmployeeDTO employee);

    Response<Void> deleteById(Long id);
}

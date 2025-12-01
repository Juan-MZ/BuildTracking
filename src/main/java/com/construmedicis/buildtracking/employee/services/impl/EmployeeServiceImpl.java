package com.construmedicis.buildtracking.employee.services.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.construmedicis.buildtracking.employee.dto.EmployeeDTO;
import com.construmedicis.buildtracking.employee.models.Employee;
import com.construmedicis.buildtracking.employee.repository.EmployeeRepository;
import com.construmedicis.buildtracking.employee.services.EmployeeService;
import com.construmedicis.buildtracking.util.exception.BusinessRuleException;
import com.construmedicis.buildtracking.util.response.Response;
import com.construmedicis.buildtracking.util.response.handler.ResponseHandler;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository repository;

    public EmployeeServiceImpl(EmployeeRepository repository) {
        this.repository = repository;
    }

    @Override
    public Response<List<EmployeeDTO>> findAll() {
        List<EmployeeDTO> dtos = repository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
        return new ResponseHandler<>(200, "Employees fetched", "/api/employees", dtos).getResponse();
    }

    @Override
    public Response<EmployeeDTO> findById(Long id) {
        Optional<Employee> opt = repository.findById(id);
        if (opt.isEmpty()) {
            throw new BusinessRuleException("employee.not.found");
        }
        return new ResponseHandler<>(200, "Employee found", "/api/employees/{id}", toDTO(opt.get())).getResponse();
    }

    @Override
    public Response<EmployeeDTO> save(EmployeeDTO employee) {
        Employee e = fromDTO(employee);
        Employee saved = repository.save(e);
        return new ResponseHandler<>(201, "Employee saved", "/api/employees", toDTO(saved)).getResponse();
    }

    @Override
    public Response<Void> deleteById(Long id) {
        if (!repository.existsById(id)) {
            throw new BusinessRuleException("employee.not.found");
        }
        repository.deleteById(id);
        return new ResponseHandler<Void>(200, "Employee deleted", "/api/employees/{id}", null).getResponse();
    }

    private EmployeeDTO toDTO(Employee e) {
        if (e == null)
            return null;
        return EmployeeDTO.builder()
                .id(e.getId())
                .identificationType(e.getIdentificationType())
                .identificationNumber(e.getIdentificationNumber())
                .birthDate(e.getBirthDate())
                .firstName(e.getFirstName())
                .lastName(e.getLastName())
                .position(e.getPosition())
                .phoneNumber(e.getPhoneNumber())
                .managerId(e.getManager() != null ? e.getManager().getId() : null)
                .build();
    }

    private Employee fromDTO(EmployeeDTO dto) {
        if (dto == null)
            return null;
        Employee e = new Employee();
        e.setId(dto.getId());
        e.setIdentificationType(dto.getIdentificationType());
        e.setIdentificationNumber(dto.getIdentificationNumber());
        e.setBirthDate(dto.getBirthDate());
        e.setFirstName(dto.getFirstName());
        e.setLastName(dto.getLastName());
        e.setPosition(dto.getPosition());
        e.setPhoneNumber(dto.getPhoneNumber());
        // associations like projects and manager are not set here to keep example
        // simple
        return e;
    }
}

package com.construmedicis.buildtracking.employee.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.construmedicis.buildtracking.employee.models.Employee;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

}

package com.construmedicis.buildtracking.employee.models;

import java.time.LocalDate;
import java.util.List;
import com.construmedicis.buildtracking.project.models.Project;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import lombok.*;
import jakarta.persistence.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "employee")
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "employee_id_seq")
    @SequenceGenerator(name = "employee_id_seq", sequenceName = "employee_id_seq", allocationSize = 1)
    @Column(name = "employee_id")
    private Long id;

    @Column(name = "employee_identification_type")
    private String identificationType;
    @Column(name = "employee_identification_number")
    private String identificationNumber;

    @Column(name = "employee_birth_date")
    private LocalDate birthDate;
    @Column(name = "employee_first_name")
    private String firstName;
    @Column(name = "employee_last_name")
    private String lastName;
    @Column(name = "employee_position")
    private String position;
    @Column(name = "employee_phone_number")
    private String phoneNumber;

    @ManyToMany
    @JoinTable(name = "employee_project", joinColumns = @JoinColumn(name = "employee_id"), inverseJoinColumns = @JoinColumn(name = "project_id"))
    private List<Project> projects;

    // may have subordinates
    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Employee manager;

    @JsonManagedReference
    @OneToMany(mappedBy = "manager", cascade = CascadeType.ALL)
    private List<Employee> subordinates;
}

package com.construmedicis.buildtracking.project.models;

import java.time.LocalDateTime;
import java.util.List;

import com.construmedicis.buildtracking.employee.models.Employee;
import com.construmedicis.buildtracking.item.models.Item;
import com.construmedicis.buildtracking.participation.models.Participation;

import lombok.*;
import jakarta.persistence.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "project")
public class Project {

    // basic attributes
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "project_id_seq")
    @SequenceGenerator(name = "project_id_seq", sequenceName = "project_id_seq", allocationSize = 1)
    @Column(name = "project_id")
    private Long id;
    @Column(name = "project_name")
    private String name;
    @Column(name = "project_description")
    private String description;
    @Column(name = "project_status")
    private String status;
    @Column(name = "project_client_name")
    private String clientName;
    @Column(name = "project_location")
    private String location;

    // dates
    @Column(name = "project_creation_date")
    private LocalDateTime creationDate;
    @Column(name = "project_start_date")
    private LocalDateTime startDate;
    @Column(name = "project_end_date")
    private LocalDateTime endDate;

    @ManyToMany(mappedBy = "projects")
    private List<Employee> employees;
    @ManyToMany(mappedBy = "projects")
    private List<Item> items;
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL)
    private List<Participation> participations;
}

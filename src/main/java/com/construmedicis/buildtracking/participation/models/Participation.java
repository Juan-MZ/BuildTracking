package com.construmedicis.buildtracking.participation.models;

import lombok.*;

import java.util.List;

import com.construmedicis.buildtracking.attendance.models.Attendance;
import com.construmedicis.buildtracking.employee.models.Employee;
import com.construmedicis.buildtracking.project.models.Project;

import jakarta.persistence.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "participation")
public class Participation {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "participation_id_seq")
    @SequenceGenerator(name = "participation_id_seq", sequenceName = "participation_id_seq", allocationSize = 1)
    @Column(name = "participation_id")
    private Long id;
    @Column(name = "participationrole")
    private String role;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "employee_id")
    private Employee employee;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "project_id")
    private Project project;

    @OneToMany(mappedBy = "participation", cascade = CascadeType.ALL)
    private List<Attendance> attendances;
}

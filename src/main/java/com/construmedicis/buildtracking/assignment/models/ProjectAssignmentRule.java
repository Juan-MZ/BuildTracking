package com.construmedicis.buildtracking.assignment.models;

import com.construmedicis.buildtracking.project.models.Project;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "project_assignment_rules")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectAssignmentRule {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "assignment_rule_seq")
    @SequenceGenerator(name = "assignment_rule_seq", sequenceName = "assignment_rule_sequence", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private Integer priority; // Orden de evaluación (menor = mayor prioridad)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RuleType ruleType;

    // Campos específicos según el tipo de regla
    @Column(length = 50)
    private String supplierNit; // Para SUPPLIER_NIT

    private LocalDate startDate; // Para DATE_RANGE
    private LocalDate endDate;   // Para DATE_RANGE

    @Column(columnDefinition = "TEXT")
    private String keywords; // JSON array para KEYWORDS, ej: ["cemento", "acero"]

    @Column(nullable = false)
    private Boolean isActive;

    @Column(length = 500)
    private String description; // Descripción de la regla para el usuario

    public enum RuleType {
        SUPPLIER_NIT,         // Asignar basado en NIT del proveedor
        DATE_RANGE,           // Asignar basado en rango de fechas de la factura
        KEYWORDS,             // Asignar basado en palabras clave en descripción de ítems
        EMPLOYEE_PARTICIPATION, // Asignar si hay empleados participando en el proyecto en esa fecha
        MANUAL                // Regla manual (siempre requiere confirmación)
    }
}

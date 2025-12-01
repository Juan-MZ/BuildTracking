package com.construmedicis.buildtracking.item.models;

import lombok.*;

import java.util.List;

import com.construmedicis.buildtracking.project.models.Project;

import jakarta.persistence.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "item")
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "item_id_seq")
    @SequenceGenerator(name = "item_id_seq", sequenceName = "item_id_seq", allocationSize = 1)
    @Column(name = "item_id")
    private Long id;
    @Column(name = "item_name")
    private String name;
    @Column(name = "item_description")
    private String description;
    @Column(name = "item_price")
    private Double price;
    @Column(name = "item_quantity")
    private Integer quantity;

    @ManyToMany
    @JoinTable(name = "item_project", joinColumns = @JoinColumn(name = "item_id"), inverseJoinColumns = @JoinColumn(name = "project_id"))
    private List<Project> projects;
}

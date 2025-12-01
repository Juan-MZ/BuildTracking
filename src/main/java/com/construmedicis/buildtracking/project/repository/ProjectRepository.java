package com.construmedicis.buildtracking.project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.construmedicis.buildtracking.project.models.Project;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

}

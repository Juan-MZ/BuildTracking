package com.construmedicis.buildtracking.assignment.repository;

import com.construmedicis.buildtracking.assignment.models.ProjectAssignmentRule;
import com.construmedicis.buildtracking.assignment.models.ProjectAssignmentRule.RuleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectAssignmentRuleRepository extends JpaRepository<ProjectAssignmentRule, Long> {

    List<ProjectAssignmentRule> findByProjectIdAndIsActiveTrue(Long projectId);

    List<ProjectAssignmentRule> findByIsActiveTrueOrderByPriorityAsc();

    List<ProjectAssignmentRule> findByRuleTypeAndIsActiveTrue(RuleType ruleType);

    List<ProjectAssignmentRule> findByProjectId(Long projectId);
}

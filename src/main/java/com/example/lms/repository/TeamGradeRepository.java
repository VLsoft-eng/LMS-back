package com.example.lms.repository;

import com.example.lms.entity.TeamGradeEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * TICKET-BE-22: Spring Data JPA repository for team_grades.
 */
public interface TeamGradeRepository extends JpaRepository<TeamGradeEntity, UUID> {

    Optional<TeamGradeEntity> findByTeamIdAndAssignmentId(UUID teamId, UUID assignmentId);

    Page<TeamGradeEntity> findAllByAssignmentId(UUID assignmentId, Pageable pageable);
}

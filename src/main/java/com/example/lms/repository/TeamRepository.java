package com.example.lms.repository;

import com.example.lms.entity.TeamEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * TICKET-BE-21: Spring Data JPA repository for teams.
 */
public interface TeamRepository extends JpaRepository<TeamEntity, UUID> {

    Page<TeamEntity> findAllByClassIdAndAssignmentId(UUID classId, UUID assignmentId, Pageable pageable);

    Page<TeamEntity> findAllByClassIdAndAssignmentIdIsNull(UUID classId, Pageable pageable);

    Page<TeamEntity> findAllByClassId(UUID classId, Pageable pageable);

    List<TeamEntity> findAllByClassIdAndAssignmentIdIsNull(UUID classId);

    List<TeamEntity> findAllByClassIdAndAssignmentId(UUID classId, UUID assignmentId);
}

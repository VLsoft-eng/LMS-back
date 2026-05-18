package com.example.lms.repository;

import com.example.lms.entity.grading.AssessmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * TICKET-BE-35b: репозиторий для ассессментов.
 */
public interface AssessmentRepository extends JpaRepository<AssessmentEntity, UUID> {

    Optional<AssessmentEntity> findBySubmissionId(UUID submissionId);

    Optional<AssessmentEntity> findByTeamGradeId(UUID teamGradeId);

    boolean existsByRubricId(UUID rubricId);

    List<AssessmentEntity> findAllByAssignmentId(UUID assignmentId);
}

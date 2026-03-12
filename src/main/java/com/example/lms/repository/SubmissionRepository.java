package com.example.lms.repository;

import com.example.lms.entity.SubmissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * TICKET-BE-03: Spring Data JPA repository for submissions.
 */
public interface SubmissionRepository extends JpaRepository<SubmissionEntity, UUID> {

	Optional<SubmissionEntity> findByAssignmentIdAndStudentId(UUID assignmentId, UUID studentId);

	List<SubmissionEntity> findAllByAssignmentId(UUID assignmentId);

	@Query("SELECT s FROM SubmissionEntity s WHERE s.assignmentId IN :assignmentIds")
	List<SubmissionEntity> findAllByAssignmentIdIn(@Param("assignmentIds") List<UUID> assignmentIds);
}

package com.example.lms.repository;

import com.example.lms.entity.CommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * TICKET-BE-03: Spring Data JPA repository for comments.
 */
public interface CommentRepository extends JpaRepository<CommentEntity, UUID> {

    List<CommentEntity> findAllByAssignmentIdOrderByCreatedAtAsc(UUID assignmentId);
}

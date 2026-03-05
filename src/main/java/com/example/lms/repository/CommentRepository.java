package com.example.lms.repository;

import com.example.lms.entity.CommentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * TICKET-BE-03: Spring Data JPA repository for comments.
 */
public interface CommentRepository extends JpaRepository<CommentEntity, UUID> {

    Page<CommentEntity> findAllByAssignmentIdOrderByCreatedAtAsc(UUID assignmentId, Pageable pageable);
}

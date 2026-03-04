package com.example.lms.repository;

import com.example.lms.entity.AssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * TICKET-BE-03: Spring Data JPA repository for assignments.
 */
public interface AssignmentRepository extends JpaRepository<AssignmentEntity, UUID> {
}

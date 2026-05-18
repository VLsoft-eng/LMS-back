package com.example.lms.repository;

import com.example.lms.entity.grading.RubricEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * TICKET-BE-35a: репозиторий для snapshot-рубрик.
 */
public interface RubricRepository extends JpaRepository<RubricEntity, UUID> {

    Optional<RubricEntity> findByAssignmentId(UUID assignmentId);

    boolean existsBySourceTemplateId(UUID sourceTemplateId);
}

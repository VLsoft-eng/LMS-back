package com.example.lms.repository;

import com.example.lms.entity.grading.RubricTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * TICKET-BE-35a: репозиторий для шаблонов рубрик.
 */
public interface RubricTemplateRepository extends JpaRepository<RubricTemplateEntity, UUID> {

    List<RubricTemplateEntity> findAllByClassIdOrderByCreatedAtDesc(UUID classId);
}

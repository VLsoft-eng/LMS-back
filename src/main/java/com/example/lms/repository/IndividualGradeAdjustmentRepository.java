package com.example.lms.repository;

import com.example.lms.entity.IndividualGradeAdjustmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * TICKET-BE-22: Spring Data JPA repository for individual_grade_adjustments.
 */
public interface IndividualGradeAdjustmentRepository extends JpaRepository<IndividualGradeAdjustmentEntity, UUID> {

    List<IndividualGradeAdjustmentEntity> findAllByTeamGradeId(UUID teamGradeId);

    Optional<IndividualGradeAdjustmentEntity> findByTeamGradeIdAndStudentId(UUID teamGradeId, UUID studentId);
}

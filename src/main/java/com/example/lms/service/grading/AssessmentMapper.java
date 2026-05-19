package com.example.lms.service.grading;

import com.example.lms.dto.grading.AssessmentDto;
import com.example.lms.dto.grading.CriterionScoreDto;
import com.example.lms.entity.grading.AssessmentEntity;
import com.example.lms.entity.grading.CriterionScoreEntity;

import java.util.List;

/**
 * TICKET-BE-38: маппинг Assessment entity → DTO.
 */
public final class AssessmentMapper {

    private AssessmentMapper() {}

    public static CriterionScoreDto toDto(CriterionScoreEntity entity) {
        return new CriterionScoreDto(
                entity.getId(),
                entity.getCriterionId(),
                entity.getBoolValue(),
                entity.getPercentValue(),
                entity.getScoreValue(),
                entity.getComputedPoints(),
                entity.getComment()
        );
    }

    public static AssessmentDto toDto(AssessmentEntity entity) {
        List<CriterionScoreDto> scores = entity.getScores().stream()
                .map(AssessmentMapper::toDto)
                .toList();
        return new AssessmentDto(
                entity.getId(),
                entity.getRubricId(),
                entity.getAssignmentId(),
                entity.getSubmissionId(),
                entity.getTeamGradeId(),
                entity.getPrimarySum(),
                entity.getBonusMultiplier(),
                entity.getFinalScore(),
                entity.getFinalScoreNormalized(),
                entity.getGradedBy(),
                entity.getGradedAt(),
                scores
        );
    }
}

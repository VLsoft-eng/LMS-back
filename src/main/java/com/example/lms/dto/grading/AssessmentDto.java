package com.example.lms.dto.grading;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Полный DTO ассессмента")
public record AssessmentDto(

        UUID id,
        UUID rubricId,
        UUID assignmentId,
        UUID submissionId,
        UUID teamGradeId,
        BigDecimal primarySum,
        BigDecimal bonusMultiplier,
        BigDecimal finalScore,
        short finalScoreNormalized,
        UUID gradedBy,
        Instant gradedAt,
        List<CriterionScoreDto> scores
) {}

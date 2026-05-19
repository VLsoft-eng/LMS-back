package com.example.lms.dto.grading;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

@Schema(description = "Запрос на создание оценивания (ровно один из submissionId / teamGradeId)")
public record CreateAssessmentRequest(

        @Schema(description = "ID сабмишена студента", nullable = true)
        UUID submissionId,

        @Schema(description = "ID командной оценки", nullable = true)
        UUID teamGradeId,

        @Schema(description = "Список оценок по критериям")
        @NotEmpty
        @Valid
        List<CriterionScoreInput> scores
) {}

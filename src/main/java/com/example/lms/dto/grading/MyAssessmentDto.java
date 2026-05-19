package com.example.lms.dto.grading;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "Студенческое представление ассессмента")
public record MyAssessmentDto(

        @Schema(description = "ID класса (для группировки и навигации в UI)")
        UUID classId,

        UUID assignmentId,
        String assignmentTitle,
        UUID assessmentId,
        BigDecimal finalScore,
        BigDecimal totalMaxPoints,
        short finalScoreNormalized,
        List<MyAssessmentCriterionDto> criteria
) {}

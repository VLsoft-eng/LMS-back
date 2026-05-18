package com.example.lms.dto.grading;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Оценка одного критерия")
public record CriterionScoreDto(

        UUID id,
        UUID criterionId,
        Boolean boolValue,
        BigDecimal percentValue,
        BigDecimal scoreValue,
        BigDecimal computedPoints,
        String comment
) {}

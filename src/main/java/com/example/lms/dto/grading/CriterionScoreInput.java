package com.example.lms.dto.grading;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Входное значение оценки одного критерия")
public record CriterionScoreInput(

        @NotNull
        UUID criterionId,

        @Schema(description = "Для kind=BOOLEAN", nullable = true)
        Boolean boolValue,

        @Schema(description = "Для kind=PERCENT (0..100)", nullable = true)
        BigDecimal percentValue,

        @Schema(description = "Для kind=SCORE (scoreMin..scoreMax)", nullable = true)
        BigDecimal scoreValue,

        @Schema(description = "Комментарий", nullable = true)
        @Size(max = 500)
        String comment
) {}

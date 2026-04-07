package com.example.lms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Запрос на корректировку индивидуальной оценки")
public record UpdateAdjustmentRequest(
        @NotNull
        @Min(-50) @Max(50)
        @Schema(description = "Корректировка -50..+50", example = "5")
        Integer adjustment,

        @Schema(description = "Комментарий", nullable = true)
        String comment
) {}

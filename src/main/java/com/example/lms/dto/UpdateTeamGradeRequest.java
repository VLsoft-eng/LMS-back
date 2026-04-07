package com.example.lms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Запрос на обновление групповой оценки")
public record UpdateTeamGradeRequest(
        @NotNull
        @Min(0) @Max(100)
        @Schema(description = "Оценка 0-100", example = "90")
        Integer grade,

        @Schema(description = "Комментарий", nullable = true)
        String comment
) {}

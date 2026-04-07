package com.example.lms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Запрос на создание групповой оценки")
public record CreateTeamGradeRequest(
        @NotNull
        @Schema(description = "ID команды")
        UUID teamId,

        @NotNull
        @Min(0) @Max(100)
        @Schema(description = "Оценка 0-100", example = "85")
        Integer grade,

        @Schema(description = "Комментарий", nullable = true)
        String comment
) {}

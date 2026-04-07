package com.example.lms.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Моя командная оценка (для студента)")
public record MyTeamGradeDto(
        @Schema(description = "ID команды")
        UUID teamId,

        @Schema(description = "Название команды")
        String teamName,

        @Schema(description = "Групповая оценка")
        int teamGrade,

        @Schema(description = "Моя корректировка")
        int myAdjustment,

        @Schema(description = "Моя итоговая оценка")
        int myFinalGrade,

        @Schema(description = "Комментарий", nullable = true)
        String comment,

        @Schema(description = "Дата выставления")
        Instant gradedAt
) {}

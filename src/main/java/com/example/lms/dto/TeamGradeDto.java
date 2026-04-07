package com.example.lms.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Групповая оценка (полное представление)")
public record TeamGradeDto(
        @Schema(description = "ID оценки")
        UUID id,

        @Schema(description = "ID команды")
        UUID teamId,

        @Schema(description = "Название команды")
        String teamName,

        @Schema(description = "ID задания")
        UUID assignmentId,

        @Schema(description = "Оценка 0-100")
        int grade,

        @Schema(description = "Комментарий", nullable = true)
        String comment,

        @Schema(description = "Индивидуальные оценки")
        List<IndividualAdjustmentDto> individualGrades,

        @Schema(description = "Кто выставил")
        UUID gradedBy,

        @Schema(description = "Дата выставления")
        Instant gradedAt
) {}

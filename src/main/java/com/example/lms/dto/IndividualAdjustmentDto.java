package com.example.lms.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Индивидуальная корректировка оценки")
public record IndividualAdjustmentDto(
        @Schema(description = "ID студента")
        UUID studentId,

        @Schema(description = "Имя студента")
        String studentName,

        @Schema(description = "Групповая оценка")
        int teamGrade,

        @Schema(description = "Корректировка")
        int adjustment,

        @Schema(description = "Итоговая оценка")
        int finalGrade,

        @Schema(description = "Комментарий", nullable = true)
        String comment,

        @Schema(description = "Кто выставил")
        UUID gradedBy,

        @Schema(description = "Дата выставления")
        Instant gradedAt
) {}

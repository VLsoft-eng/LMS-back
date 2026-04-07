package com.example.lms.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Групповая оценка (краткое для списка)")
public record TeamGradeListItemDto(
        @Schema(description = "ID оценки")
        UUID id,

        @Schema(description = "ID команды")
        UUID teamId,

        @Schema(description = "Название команды")
        String teamName,

        @Schema(description = "Оценка")
        int grade,

        @Schema(description = "Количество участников")
        int memberCount,

        @Schema(description = "Дата выставления")
        Instant gradedAt
) {}

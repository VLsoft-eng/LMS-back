package com.example.lms.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Быстрое задание")
public record QuickAssignmentDto(
        @Schema(description = "ID задания")
        UUID id,

        @Schema(description = "ID класса")
        UUID classId,

        @Schema(description = "Название задания")
        String title,

        @Schema(description = "Тип задания")
        String type,

        @Schema(description = "Командное задание")
        boolean isTeamBased,

        @Schema(description = "Привязанные команды", nullable = true)
        List<TeamShortDto> teams,

        @Schema(description = "ID создателя")
        UUID createdBy,

        @Schema(description = "Дата создания")
        Instant createdAt
) {}

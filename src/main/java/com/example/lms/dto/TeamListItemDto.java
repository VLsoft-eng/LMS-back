package com.example.lms.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Команда (краткое представление для списка)")
public record TeamListItemDto(
        @Schema(description = "ID команды")
        UUID id,

        @Schema(description = "Название команды")
        String name,

        @Schema(description = "ID задания (null = постоянная)", nullable = true)
        UUID assignmentId,

        @Schema(description = "Количество участников")
        int memberCount,

        @Schema(description = "Имя лидера", nullable = true)
        String leaderName,

        @Schema(description = "Дата создания")
        Instant createdAt
) {}

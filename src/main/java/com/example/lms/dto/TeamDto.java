package com.example.lms.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Команда (полное представление)")
public record TeamDto(
        @Schema(description = "ID команды")
        UUID id,

        @Schema(description = "ID класса")
        UUID classId,

        @Schema(description = "ID задания (null = постоянная)", nullable = true)
        UUID assignmentId,

        @Schema(description = "Название команды")
        String name,

        @Schema(description = "Участники команды")
        List<TeamMemberDto> members,

        @Schema(description = "ID создателя")
        UUID createdBy,

        @Schema(description = "Дата создания")
        Instant createdAt
) {}

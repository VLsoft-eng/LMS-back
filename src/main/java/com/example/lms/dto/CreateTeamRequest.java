package com.example.lms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

@Schema(description = "Запрос на создание команды")
public record CreateTeamRequest(
        @NotBlank
        @Size(max = 100)
        @Schema(description = "Название команды", example = "Ансамбль A")
        String name,

        @Schema(description = "ID задания (null = постоянная команда)", nullable = true)
        UUID assignmentId,

        @NotEmpty
        @Schema(description = "ID участников команды")
        List<UUID> memberUserIds,

        @Schema(description = "ID лидера команды (опционально)", nullable = true)
        UUID leaderUserId
) {}

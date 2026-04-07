package com.example.lms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "Запрос на обновление команды")
public record UpdateTeamRequest(
        @Size(max = 100)
        @Schema(description = "Новое название команды", nullable = true)
        String name,

        @Schema(description = "ID нового лидера", nullable = true)
        UUID leaderUserId
) {}

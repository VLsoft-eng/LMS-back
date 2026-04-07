package com.example.lms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Запрос на добавление участника в команду")
public record AddTeamMemberRequest(
        @NotNull
        @Schema(description = "ID пользователя")
        UUID userId,

        @Schema(description = "Является ли лидером")
        boolean isLeader
) {}

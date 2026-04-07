package com.example.lms.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Участник команды")
public record TeamMemberDto(
        @Schema(description = "ID пользователя")
        UUID userId,

        @Schema(description = "Имя", example = "Иван")
        String firstName,

        @Schema(description = "Фамилия", example = "Петров")
        String lastName,

        @Schema(description = "Является лидером")
        boolean isLeader,

        @Schema(description = "Дата вступления")
        Instant joinedAt
) {}

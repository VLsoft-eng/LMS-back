package com.example.lms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

@Schema(description = "Запрос на создание быстрого задания")
public record CreateQuickAssignmentRequest(
        @NotBlank
        @Schema(description = "Название задания", example = "Спеть 'Катюшу' в ансамбле")
        String title,

        @Schema(description = "Командное задание")
        boolean isTeamBased,

        @Schema(description = "ID команд (если командное)", nullable = true)
        List<UUID> teamIds
) {}

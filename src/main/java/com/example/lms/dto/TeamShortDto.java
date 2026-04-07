package com.example.lms.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Краткая информация о команде")
public record TeamShortDto(
        @Schema(description = "ID команды")
        UUID id,

        @Schema(description = "Название команды")
        String name
) {}

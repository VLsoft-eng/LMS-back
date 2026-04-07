package com.example.lms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Запрос на автоматическую разбивку на команды")
public record ShuffleRequest(
        @NotNull
        @Min(2)
        @Schema(description = "Количество команд", example = "3")
        Integer teamCount,

        @Schema(description = "ID задания (null = постоянные команды)", nullable = true)
        UUID assignmentId,

        @NotNull
        @Schema(description = "Стратегия распределения", example = "RANDOM", allowableValues = {"RANDOM", "BALANCED"})
        String strategy
) {}

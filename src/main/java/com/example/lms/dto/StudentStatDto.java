package com.example.lms.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Статистика одного студента в классе")
public record StudentStatDto(

        @Schema(description = "ID студента")
        UUID studentId,

        @Schema(description = "Имя студента", example = "Иван Иванов")
        String studentName,

        @Schema(description = "URL аватарки", nullable = true)
        String avatarUrl,

        @Schema(description = "Количество сданных заданий", example = "7")
        int submittedCount,

        @Schema(description = "Количество оценённых заданий", example = "5")
        int gradedCount,

        @Schema(description = "Средняя оценка (null если нет оценённых работ)", example = "82.5", nullable = true)
        Double averageGrade,

        @Schema(description = "Количество пропущенных заданий (дедлайн прошёл, ответ не сдан)", example = "2")
        int missedCount
) {}

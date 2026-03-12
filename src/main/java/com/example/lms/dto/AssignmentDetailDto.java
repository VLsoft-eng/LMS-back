package com.example.lms.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Детальное представление задания")
public record AssignmentDetailDto(
        @Schema(description = "ID задания", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "ID класса", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID classId,

        @Schema(description = "Заголовок задания", example = "Домашнее задание №1")
        String title,

        @Schema(description = "Описание задания", example = "Решить задачи 1–10 из учебника", nullable = true)
        String description,

        @Schema(description = "ID создателя задания")
        UUID createdBy,

        @Schema(description = "Имя создателя задания", example = "Иван Иванов")
        String createdByName,

        @Schema(description = "Дедлайн (UTC). null — без дедлайна.", nullable = true)
        Instant deadline,

        @Schema(description = "Дата создания задания")
        Instant createdAt,

        @Schema(description = "Статус ответа текущего студента. null — для OWNER/TEACHER или если ответ не сдан.", example = "SUBMITTED", nullable = true,
                allowableValues = {"NOT_SUBMITTED", "SUBMITTED", "GRADED"})
        String submissionStatus,

        @Schema(description = "Оценка студента (0–100). null — не оценено или пользователь не студент.", example = "85", nullable = true)
        Integer grade,

        @Schema(description = "Список URL прикреплённых файлов", nullable = true)
        List<String> fileUrls
) {}

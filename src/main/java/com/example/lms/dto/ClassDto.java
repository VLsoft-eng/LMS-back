package com.example.lms.dto;

import com.example.lms.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Класс (учебная группа)")
public record ClassDto(
        @Schema(description = "Уникальный ID класса", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Название класса", example = "Математика 10А")
        String name,

        @Schema(description = "Реферальный код для вступления (8 символов). Виден только OWNER и TEACHER.", example = "AB12CD34", nullable = true)
        String code,

        @Schema(description = "Роль текущего пользователя в классе", example = "STUDENT")
        Role myRole,

        @Schema(description = "Количество участников", example = "25")
        int memberCount,

        @Schema(description = "Дата создания класса")
        Instant createdAt
) {}

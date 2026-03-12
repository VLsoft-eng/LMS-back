package com.example.lms.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Профиль пользователя")
public record UserDto(
        @Schema(description = "Уникальный ID пользователя", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Имя", example = "Иван")
        String firstName,

        @Schema(description = "Фамилия", example = "Иванов")
        String lastName,

        @Schema(description = "Email", example = "ivan@example.com")
        String email,

        @Schema(description = "URL аватарки", example = "http://localhost:8080/api/v1/files/avatar.jpeg", nullable = true)
        String avatarUrl,

        @Schema(description = "Дата рождения", example = "2000-01-15", nullable = true)
        LocalDate dateOfBirth,

        @Schema(description = "Дата регистрации")
        Instant createdAt
) {}

package com.example.lms.dto;

import com.example.lms.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Участник класса")
public record MemberDto(
        @Schema(description = "ID пользователя", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID userId,

        @Schema(description = "Имя", example = "Иван")
        String firstName,

        @Schema(description = "Фамилия", example = "Иванов")
        String lastName,

        @Schema(description = "Email", example = "ivan@example.com")
        String email,

        @Schema(description = "Роль в классе", example = "STUDENT")
        Role role,

        @Schema(description = "Дата вступления в класс")
        Instant joinedAt,

        @Schema(description = "URL аватарки", nullable = true, example = "http://localhost:8080/api/v1/files/avatar.jpeg")
        String avatarUrl
) {}

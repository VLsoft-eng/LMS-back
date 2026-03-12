package com.example.lms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "Запрос на регистрацию")
public record RegisterRequest(

        @Schema(description = "Имя", example = "Иван")
        @NotBlank
        String firstName,

        @Schema(description = "Фамилия", example = "Иванов")
        @NotBlank
        String lastName,

        @Schema(description = "Email (уникальный)", example = "ivan@example.com")
        @NotBlank
        @Email
        String email,

        @Schema(description = "Пароль (минимум 8 символов)", example = "secret123")
        @NotBlank
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        @Schema(description = "Дата рождения", example = "2000-01-15", nullable = true)
        LocalDate dateOfBirth,

        @Schema(description = "Аватарка в формате base64 (опционально)", nullable = true)
        String avatarBase64
) {}

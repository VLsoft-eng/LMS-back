package com.example.lms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "Запрос на обновление профиля. Email изменить нельзя.")
public record UpdateProfileRequest(

        @Schema(description = "Имя", example = "Иван")
        @NotBlank
        @Size(max = 100)
        String firstName,

        @Schema(description = "Фамилия", example = "Иванов")
        @NotBlank
        @Size(max = 100)
        String lastName,

        @Schema(description = "URL аватарки (опционально). Если null — не обновляется.", example = "https://example.com/avatar.jpg", nullable = true)
        @Size(max = 500)
        String avatarUrl,

        @Schema(description = "Дата рождения", example = "2000-01-15", nullable = true)
        LocalDate dateOfBirth
) {}

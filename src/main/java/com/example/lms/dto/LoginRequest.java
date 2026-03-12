package com.example.lms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Запрос на вход")
public record LoginRequest(

        @Schema(description = "Email пользователя", example = "ivan@example.com")
        @NotBlank
        @Email
        String email,

        @Schema(description = "Пароль", example = "secret123")
        @NotBlank
        String password
) {}

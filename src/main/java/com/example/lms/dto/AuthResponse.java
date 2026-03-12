package com.example.lms.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ответ на успешную аутентификацию")
public record AuthResponse(

        @Schema(description = "JWT-токен для авторизации запросов", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String token,

        @Schema(description = "Данные аутентифицированного пользователя")
        UserDto user
) {}

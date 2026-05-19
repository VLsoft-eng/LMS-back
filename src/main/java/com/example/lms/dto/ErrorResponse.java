package com.example.lms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Стандартный ответ при ошибке")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(

        @Schema(description = "Время ошибки (UTC)")
        Instant timestamp,

        @Schema(description = "HTTP-статус код", example = "404")
        int status,

        @Schema(description = "Краткое описание ошибки", example = "Not Found")
        String error,

        @Schema(description = "Машиночитаемый код ошибки (для бизнес-ошибок)", example = "RUBRIC_REQUIRED", nullable = true)
        String code,

        @Schema(description = "Подробное сообщение об ошибке", example = "Class not found: 3fa85f64-...")
        String message,

        @Schema(description = "Путь запроса", example = "/api/v1/classes/3fa85f64-...")
        String path
) {
    public ErrorResponse(Instant timestamp, int status, String error, String message, String path) {
        this(timestamp, status, error, null, message, path);
    }
}

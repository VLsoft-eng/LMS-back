package com.example.lms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Запрос на добавление комментария")
public record AddCommentRequest(

        @Schema(description = "Текст комментария", example = "Отличная работа!")
        @NotBlank(message = "Text is required")
        String text
) {}

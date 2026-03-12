package com.example.lms.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Комментарий к заданию")
public record CommentDto(
        @Schema(description = "ID комментария", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "ID задания")
        UUID assignmentId,

        @Schema(description = "ID автора")
        UUID authorId,

        @Schema(description = "Имя автора", example = "Иван Иванов")
        String authorName,

        @Schema(description = "URL аватарки автора", nullable = true, example = "http://localhost:8080/api/v1/files/avatar.jpeg")
        String authorAvatarUrl,

        @Schema(description = "Текст комментария", example = "Отличная работа!")
        String text,

        @Schema(description = "Дата и время публикации")
        Instant createdAt
) {}

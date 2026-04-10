package com.example.lms.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Ответ студента на задание")
public record SubmissionDto(
        @Schema(description = "ID ответа", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "ID студента")
        UUID studentId,

        @Schema(description = "Имя студента", example = "Иван Иванов")
        String studentName,

        @Schema(description = "URL аватарки студента", nullable = true, example = "http://localhost:8080/api/v1/files/avatar.jpeg")
        String studentAvatarUrl,

        @Schema(description = "Текст ответа", nullable = true, example = "Мой ответ на задание...")
        String answerText,

        @Schema(description = "Список URL прикреплённых файлов", nullable = true)
        List<String> fileUrls,

        @Schema(description = "Оценка (0–100). null — ещё не оценено.", example = "85", nullable = true)
        Integer grade,

        @Schema(description = "Дата и время сдачи ответа")
        Instant submittedAt,

        @Schema(description = "Название команды (только для командных заданий)", nullable = true)
        String teamName
) {}

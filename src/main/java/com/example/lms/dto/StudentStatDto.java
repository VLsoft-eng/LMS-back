package com.example.lms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Статистика одного студента в классе")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StudentStatDto(

        @Schema(description = "ID студента")
        UUID studentId,

        @Schema(description = "Имя студента", example = "Иван Иванов")
        String studentName,

        @Schema(description = "URL аватарки", nullable = true)
        String avatarUrl,

        @Schema(description = "Количество сданных заданий", example = "7")
        int submittedCount,

        @Schema(description = "Количество оценённых заданий", example = "5")
        int gradedCount,

        @Schema(description = "Средняя оценка (null если нет оценённых работ)", example = "82.5", nullable = true)
        Double averageGrade,

        @Schema(description = "Количество пропущенных заданий (дедлайн прошёл, ответ не сдан)", example = "2")
        int missedCount,

        @Schema(description = "Среднее значение рубричного finalScore (если есть)",
                example = "8.7", nullable = true)
        BigDecimal rubricFinalScore,

        @Schema(description = "Суммарный totalMaxPoints (для отображения '8.7 / 10')",
                example = "10.00", nullable = true)
        BigDecimal totalMaxPoints
) {
    public StudentStatDto(UUID studentId, String studentName, String avatarUrl,
                          int submittedCount, int gradedCount, Double averageGrade,
                          int missedCount) {
        this(studentId, studentName, avatarUrl, submittedCount, gradedCount,
                averageGrade, missedCount, null, null);
    }
}

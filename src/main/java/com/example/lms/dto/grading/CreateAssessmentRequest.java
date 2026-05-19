package com.example.lms.dto.grading;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

@Schema(description = "Запрос на создание оценивания (ровно один из submissionId / teamId / teamGradeId)")
public record CreateAssessmentRequest(

        @Schema(description = "ID сабмишена студента (индивидуальное оценивание)", nullable = true)
        UUID submissionId,

        @Schema(description = "ID команды (командное оценивание). Сервер атомарно создаёт TeamGrade " +
                "и индивидуальные корректировки для всех членов команды.", nullable = true)
        UUID teamId,

        @Deprecated
        @Schema(description = "Устаревшее. ID уже существующей командной оценки. " +
                "Сохранён для совместимости с заданиями без рубрики, где TeamGrade создан через legacy-эндпойнт. " +
                "Для новых сценариев используйте teamId.", nullable = true)
        UUID teamGradeId,

        @Schema(description = "Список оценок по критериям")
        @NotEmpty
        @Valid
        List<CriterionScoreInput> scores
) {}

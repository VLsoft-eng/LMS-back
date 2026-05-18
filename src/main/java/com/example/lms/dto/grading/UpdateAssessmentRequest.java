package com.example.lms.dto.grading;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "Запрос на обновление оценивания (пересчёт)")
public record UpdateAssessmentRequest(

        @NotEmpty
        @Valid
        List<CriterionScoreInput> scores
) {}

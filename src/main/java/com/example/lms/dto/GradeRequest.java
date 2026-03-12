package com.example.lms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Запрос на выставление оценки")
public record GradeRequest(

        @Schema(description = "Оценка от 0 до 100", example = "85")
        @NotNull(message = "Grade is required")
        @Min(value = 0, message = "Grade must be at least 0")
        @Max(value = 100, message = "Grade must be at most 100")
        Integer grade
) {}

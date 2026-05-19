package com.example.lms.dto.grading;

import com.example.lms.entity.grading.CriterionKind;
import com.example.lms.entity.grading.CriterionRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Входной критерий шаблона рубрики")
public record CriterionTemplateInput(

        @Schema(description = "Порядковый номер критерия (≥ 0)", example = "0")
        @NotNull
        @PositiveOrZero
        Integer ordinal,

        @Schema(description = "Название критерия", example = "Код компилируется")
        @NotBlank
        @Size(max = 200)
        String title,

        @Schema(description = "Описание критерия", nullable = true)
        @Size(max = 2000)
        String description,

        @Schema(description = "Тип значения (BOOLEAN/PERCENT/SCORE)")
        @NotNull
        CriterionKind kind,

        @Schema(description = "Роль критерия (PRIMARY/BONUS)")
        @NotNull
        CriterionRole role,

        @Schema(description = "Максимум баллов (обязателен для PRIMARY)", nullable = true)
        BigDecimal maxPoints,

        @Schema(description = "Максимальный коэффициент (обязателен для BONUS, 1.0001..2.0000)", nullable = true)
        BigDecimal maxCoefficient,

        @Schema(description = "Минимум диапазона (только для kind=SCORE)", nullable = true)
        BigDecimal scoreMin,

        @Schema(description = "Максимум диапазона (только для kind=SCORE)", nullable = true)
        BigDecimal scoreMax
) {}

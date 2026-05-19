package com.example.lms.dto.grading;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Запрос на создание шаблона рубрики")
public record CreateRubricTemplateRequest(

        @Schema(description = "Название шаблона", example = "Лабораторная — стандартная рубрика")
        @NotBlank
        @Size(max = 200)
        String name,

        @Schema(description = "Описание шаблона", nullable = true)
        @Size(max = 2000)
        String description,

        @Schema(description = "Максимум баллов (> 0, ≤ 1000)", example = "10.00")
        @NotNull
        BigDecimal totalMaxPoints,

        @Schema(description = "Разрешить выход за пределы maxPoints за счёт бонусов")
        boolean allowOvercap,

        @Schema(description = "Список критериев (от 1 до 50)")
        @NotEmpty
        @Size(min = 1, max = 50)
        @Valid
        List<CriterionTemplateInput> criteria
) {}

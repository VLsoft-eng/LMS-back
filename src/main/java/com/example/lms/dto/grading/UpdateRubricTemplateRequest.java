package com.example.lms.dto.grading;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Запрос на полную замену шаблона рубрики")
public record UpdateRubricTemplateRequest(

        @NotBlank
        @Size(max = 200)
        String name,

        @Size(max = 2000)
        String description,

        @NotNull
        BigDecimal totalMaxPoints,

        boolean allowOvercap,

        @NotEmpty
        @Size(min = 1, max = 50)
        @Valid
        List<CriterionTemplateInput> criteria
) {}

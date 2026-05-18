package com.example.lms.dto.grading;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Шаблон рубрики (полный DTO)")
public record RubricTemplateDto(

        UUID id,
        UUID classId,
        String name,
        String description,
        BigDecimal totalMaxPoints,
        boolean allowOvercap,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt,
        List<CriterionTemplateDto> criteria
) {}

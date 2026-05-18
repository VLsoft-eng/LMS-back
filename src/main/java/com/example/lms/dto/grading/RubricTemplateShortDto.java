package com.example.lms.dto.grading;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Краткое представление шаблона рубрики (для списка)")
public record RubricTemplateShortDto(

        UUID id,
        String name,
        BigDecimal totalMaxPoints,
        int criteriaCount,
        Instant createdAt
) {}

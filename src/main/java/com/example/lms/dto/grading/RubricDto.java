package com.example.lms.dto.grading;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Рубрика (snapshot, привязанный к assignment)")
public record RubricDto(

        UUID id,
        UUID assignmentId,
        UUID sourceTemplateId,
        String name,
        String description,
        BigDecimal totalMaxPoints,
        boolean allowOvercap,
        Instant frozenAt,
        List<CriterionDto> criteria
) {}

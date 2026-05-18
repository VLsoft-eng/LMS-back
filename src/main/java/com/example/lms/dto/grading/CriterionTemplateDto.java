package com.example.lms.dto.grading;

import com.example.lms.entity.grading.CriterionKind;
import com.example.lms.entity.grading.CriterionRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Критерий-шаблон")
public record CriterionTemplateDto(

        UUID id,
        int ordinal,
        String title,
        String description,
        CriterionKind kind,
        CriterionRole role,
        BigDecimal maxPoints,
        BigDecimal maxCoefficient,
        BigDecimal scoreMin,
        BigDecimal scoreMax
) {}

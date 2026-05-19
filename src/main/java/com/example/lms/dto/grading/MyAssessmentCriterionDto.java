package com.example.lms.dto.grading;

import com.example.lms.entity.grading.CriterionKind;
import com.example.lms.entity.grading.CriterionRole;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

@Schema(description = "Раскрытие критерия в карточке студента")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MyAssessmentCriterionDto(

        String title,
        CriterionKind kind,
        CriterionRole role,
        Object value,
        BigDecimal maxPoints,
        BigDecimal maxCoefficient,
        BigDecimal scoreMin,
        BigDecimal scoreMax,
        BigDecimal computedPoints,
        String comment
) {}

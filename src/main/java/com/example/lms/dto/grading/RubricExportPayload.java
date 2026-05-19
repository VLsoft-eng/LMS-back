package com.example.lms.dto.grading;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Формат импорта/экспорта шаблона рубрики (см. PRD §5)")
public record RubricExportPayload(

        @JsonProperty("$schema")
        String schema,

        @Schema(description = "Версия формата (whitelist: 1.0)")
        String version,

        Instant exportedAt,

        RubricExportBody rubric
) {

    @Schema(description = "Тело экспортируемой рубрики (без id и серверных полей)")
    public record RubricExportBody(
            String name,
            String description,
            java.math.BigDecimal totalMaxPoints,
            boolean allowOvercap,
            java.util.List<CriterionTemplateInput> criteria
    ) {}
}

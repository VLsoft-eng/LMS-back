package com.example.lms.dto.grading;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;

import java.util.UUID;

@Schema(description = "Запрос на привязку рубрики к заданию (один из fromTemplateId / adhoc)")
public record AttachRubricRequest(

        @Schema(description = "ID шаблона рубрики", nullable = true)
        UUID fromTemplateId,

        @Schema(description = "Описание ad-hoc рубрики", nullable = true)
        @Valid
        AdhocRubricInput adhoc
) {}

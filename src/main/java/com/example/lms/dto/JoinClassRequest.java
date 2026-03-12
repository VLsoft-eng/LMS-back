package com.example.lms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Запрос на вступление в класс")
public record JoinClassRequest(

        @Schema(description = "Реферальный код класса (8 символов)", example = "AB12CD34")
        @NotBlank
        String code
) {}

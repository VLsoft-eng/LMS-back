package com.example.lms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Запрос на создание класса")
public record CreateClassRequest(

        @Schema(description = "Название класса (макс. 255 символов)", example = "Математика 10А")
        @NotBlank
        @Size(max = 255)
        String name
) {}

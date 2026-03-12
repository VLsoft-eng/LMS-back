package com.example.lms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Запрос на обновление класса")
public record UpdateClassRequest(

        @Schema(description = "Новое название класса (макс. 255 символов)", example = "Физика 11Б")
        @NotBlank
        @Size(max = 255)
        String name
) {}

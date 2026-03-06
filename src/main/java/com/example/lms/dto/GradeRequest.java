package com.example.lms.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record GradeRequest(
        @NotNull(message = "Grade is required")
        @Min(value = 0, message = "Grade must be at least 0")
        @Max(value = 100, message = "Grade must be at most 100")
        Integer grade
) {}

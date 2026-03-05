package com.example.lms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateClassRequest(

        @NotBlank
        @Size(max = 255)
        String name
) {}

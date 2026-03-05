package com.example.lms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateClassRequest(

        @NotBlank
        @Size(max = 255)
        String name
) {}

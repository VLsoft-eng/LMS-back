package com.example.lms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateAssignmentRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 255)
        String title,

        String description,

        Instant deadline
) {}

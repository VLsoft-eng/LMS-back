package com.example.lms.dto;

import jakarta.validation.constraints.NotBlank;

public record AddCommentRequest(

        @NotBlank(message = "Text is required")
        String text
) {}

package com.example.lms.dto;

import jakarta.validation.constraints.NotBlank;

public record JoinClassRequest(

        @NotBlank
        String code
) {}

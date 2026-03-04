package com.example.lms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * TICKET-BE-06: Payload for PUT /api/v1/users/me.
 * Email is intentionally absent — it is read-only and cannot be changed.
 */
public record UpdateProfileRequest(

        @NotBlank
        @Size(max = 100)
        String firstName,

        @NotBlank
        @Size(max = 100)
        String lastName,

        @Size(max = 500)
        String avatarUrl,

        LocalDate dateOfBirth
) {}

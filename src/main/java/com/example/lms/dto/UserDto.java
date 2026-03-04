package com.example.lms.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * TICKET-BE-06: Read-only view of a user profile returned to the client.
 */
public record UserDto(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String avatarUrl,
        LocalDate dateOfBirth,
        Instant createdAt
) {}

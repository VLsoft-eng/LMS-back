package com.example.lms.dto;

import java.time.Instant;
import java.util.UUID;

public record MemberDto(
        UUID userId,
        String firstName,
        String lastName,
        String email,
        String role,
        Instant joinedAt
) {}

package com.example.lms.dto;

import com.example.lms.entity.Role;

import java.time.Instant;
import java.util.UUID;

public record MemberDto(
        UUID userId,
        String firstName,
        String lastName,
        String email,
        Role role,
        Instant joinedAt
) {}

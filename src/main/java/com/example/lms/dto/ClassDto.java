package com.example.lms.dto;

import com.example.lms.entity.Role;

import java.time.Instant;
import java.util.UUID;

public record ClassDto(
        UUID    id,
        String  name,
        String  code,
        Role    myRole,
        int     memberCount,
        Instant createdAt
) {}

package com.example.lms.dto;

import java.time.Instant;
import java.util.UUID;

public record ClassDto(
        UUID    id,
        String  name,
        String  code,
        String  myRole,
        int     memberCount,
        Instant createdAt
) {}

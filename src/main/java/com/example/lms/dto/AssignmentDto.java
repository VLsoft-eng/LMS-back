package com.example.lms.dto;

import java.time.Instant;
import java.util.UUID;

public record AssignmentDto(
        UUID id,
        String title,
        String description,
        Instant deadline,
        Instant createdAt,
        String submissionStatus,
        Integer grade
) {}

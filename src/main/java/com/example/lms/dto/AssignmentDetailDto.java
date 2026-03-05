package com.example.lms.dto;

import java.time.Instant;
import java.util.UUID;

public record AssignmentDetailDto(
        UUID id,
        UUID classId,
        String title,
        String description,
        UUID createdBy,
        String createdByName,
        Instant createdAt,
        String submissionStatus,
        Integer grade
) {}

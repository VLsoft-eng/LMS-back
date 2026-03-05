package com.example.lms.dto;

import java.time.Instant;
import java.util.UUID;

public record CommentDto(
        UUID id,
        UUID assignmentId,
        UUID authorId,
        String authorName,
        String text,
        Instant createdAt
) {}

package com.example.lms.dto.peerreview;

import java.time.Instant;
import java.util.UUID;

/**
 * TICKET #9181
 */
public record PeerReviewSettingsDto(
        UUID id,
        UUID assignmentId,
        int reviewsPerStudent,
        boolean isEnabled,
        Instant dueDate,
        Instant createdAt,
        Instant updatedAt
) {}

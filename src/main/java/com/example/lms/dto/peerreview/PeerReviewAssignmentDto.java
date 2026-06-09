package com.example.lms.dto.peerreview;

import com.example.lms.entity.peerreview.PeerReviewStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * TICKET #9181: назначение «рецензент → submission».
 */
public record PeerReviewAssignmentDto(
        UUID id,
        UUID assignmentId,
        UUID submissionId,
        String studentName,
        String studentAvatarUrl,
        PeerReviewStatus status,
        Instant createdAt
) {}

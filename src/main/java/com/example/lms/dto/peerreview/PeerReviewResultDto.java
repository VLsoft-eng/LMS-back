package com.example.lms.dto.peerreview;

import java.util.List;
import java.util.UUID;

/**
 * TICKET #9181: агрегированный результат peer-review по конкретной работе (для учителя).
 */
public record PeerReviewResultDto(
        UUID submissionId,
        String studentName,
        Double averageScore,
        int assessmentCount,
        List<PeerAssessmentDto> assessments
) {}

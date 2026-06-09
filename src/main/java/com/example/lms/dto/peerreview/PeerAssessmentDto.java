package com.example.lms.dto.peerreview;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * TICKET #9181: оценка, выставленная студентом-рецензентом.
 * reviewerId намеренно отсутствует для анонимизации при показе оцениваемому студенту.
 */
public record PeerAssessmentDto(
        UUID id,
        UUID peerReviewAssignmentId,
        UUID rubricId,
        BigDecimal primarySum,
        BigDecimal bonusMultiplier,
        BigDecimal finalScore,
        short finalScoreNormalized,
        Instant submittedAt,
        List<PeerCriterionScoreDto> scores
) {}

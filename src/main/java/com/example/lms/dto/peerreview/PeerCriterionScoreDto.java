package com.example.lms.dto.peerreview;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * TICKET #9181
 */
public record PeerCriterionScoreDto(
        UUID id,
        UUID criterionId,
        Boolean boolValue,
        BigDecimal percentValue,
        BigDecimal scoreValue,
        BigDecimal computedPoints,
        String comment
) {}

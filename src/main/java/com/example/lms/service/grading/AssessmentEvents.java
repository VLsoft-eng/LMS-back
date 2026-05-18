package com.example.lms.service.grading;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * TICKET-BE-38: доменные события ассессмента (Spring ApplicationEventPublisher).
 */
public final class AssessmentEvents {

    private AssessmentEvents() {}

    public record AssessmentCreated(
            UUID assessmentId,
            UUID submissionId,
            UUID teamGradeId,
            BigDecimal finalScore
    ) {}

    public record AssessmentUpdated(
            UUID assessmentId,
            BigDecimal oldFinalScore,
            BigDecimal newFinalScore
    ) {}

    public record AssessmentDeleted(
            UUID assessmentId,
            UUID submissionId,
            UUID teamGradeId
    ) {}
}

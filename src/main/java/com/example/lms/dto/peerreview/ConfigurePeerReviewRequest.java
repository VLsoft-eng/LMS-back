package com.example.lms.dto.peerreview;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * TICKET #9181: запрос на конфигурацию/включение peer-review для задания.
 */
public record ConfigurePeerReviewRequest(
        @NotNull @Min(1) Integer reviewsPerStudent,
        Instant dueDate
) {}

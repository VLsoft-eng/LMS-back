package com.example.lms.dto.peerreview;

import com.example.lms.dto.grading.CriterionScoreInput;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * TICKET #9181: запрос на отправку peer-оценки по критериям рубрики.
 */
public record SubmitPeerAssessmentRequest(
        @NotNull @NotEmpty List<CriterionScoreInput> scores
) {}

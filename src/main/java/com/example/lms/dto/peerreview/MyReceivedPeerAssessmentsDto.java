package com.example.lms.dto.peerreview;

import java.util.List;
import java.util.UUID;

/**
 * TICKET #9181: peer-оценки, полученные студентом за свою работу.
 * Анонимизированы — reviewerId не передаётся.
 */
public record MyReceivedPeerAssessmentsDto(
        UUID submissionId,
        UUID assignmentId,
        List<PeerAssessmentDto> assessments
) {}

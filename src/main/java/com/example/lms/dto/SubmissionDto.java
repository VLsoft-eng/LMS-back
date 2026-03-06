package com.example.lms.dto;

import java.time.Instant;
import java.util.UUID;

public record SubmissionDto(
        UUID id,
        UUID studentId,
        String studentName,
        String answerText,
        String fileUrl,
        Integer grade,
        Instant submittedAt
) {}

package com.example.lms.dto;

import java.time.Instant;

/**
 * TICKET-BE-13 (stub): Uniform error envelope returned by GlobalExceptionHandler.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {}

package com.example.lms.exception;

/**
 * TICKET-BE-06 / TICKET-BE-13: Thrown when a requested resource does not exist.
 * Maps to HTTP 404 in GlobalExceptionHandler.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}

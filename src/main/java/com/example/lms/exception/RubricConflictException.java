package com.example.lms.exception;

/**
 * TICKET-BE-31: конфликт состояния рубрики/ассессмента (409).
 * code — машиночитаемый идентификатор (см. словарь ошибок PRD §10.1).
 */
public class RubricConflictException extends RuntimeException {

    private final String code;

    public RubricConflictException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

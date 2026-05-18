package com.example.lms.exception;

/**
 * TICKET-BE-31: бизнес-инварианты рубрики/ассессмента нарушены.
 * code — машиночитаемый идентификатор (см. словарь ошибок PRD §10.1).
 */
public class RubricInvariantViolation extends RuntimeException {

    private final String code;

    public RubricInvariantViolation(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

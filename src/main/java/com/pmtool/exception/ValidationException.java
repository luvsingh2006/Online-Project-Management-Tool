package com.pmtool.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Thrown when business rules or input validation constraints are violated.
 */
public class ValidationException extends AppException {

    private final Map<String, String> fieldErrors;

    public ValidationException(String message) {
        super(ErrorCode.VALIDATION_FAILED, message);
        this.fieldErrors = new LinkedHashMap<>();
    }

    public ValidationException(String message, Map<String, String> fieldErrors) {
        super(ErrorCode.VALIDATION_FAILED, message);
        this.fieldErrors = new LinkedHashMap<>(fieldErrors);
        addDetail("fieldErrors", this.fieldErrors);
    }

    public ValidationException addFieldError(String field, String error) {
        this.fieldErrors.put(field, error);
        addDetail("fieldErrors", this.fieldErrors);
        return this;
    }

    public Map<String, String> getFieldErrors() {
        return Collections.unmodifiableMap(fieldErrors);
    }

    public boolean hasErrors() {
        return !fieldErrors.isEmpty();
    }
}

package com.pmtool.exception;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Base abstract application exception demonstrating OOP inheritance and structured error reporting.
 */
public abstract class AppException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Instant timestamp;
    private final Map<String, Object> details;

    public AppException(ErrorCode errorCode, String message) {
        super(message != null ? message : errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.timestamp = Instant.now();
        this.details = new HashMap<>();
    }

    public AppException(ErrorCode errorCode, String message, Throwable cause) {
        super(message != null ? message : errorCode.getDefaultMessage(), cause);
        this.errorCode = errorCode;
        this.timestamp = Instant.now();
        this.details = new HashMap<>();
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public int getHttpStatus() {
        return errorCode.getHttpStatus();
    }

    public Map<String, Object> getDetails() {
        return Collections.unmodifiableMap(details);
    }

    public AppException addDetail(String key, Object value) {
        this.details.put(key, value);
        return this;
    }
}

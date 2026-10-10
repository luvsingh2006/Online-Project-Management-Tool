package com.pmtool.exception;

/**
 * Standardized application error codes with corresponding HTTP status codes.
 */
public enum ErrorCode {
    RESOURCE_NOT_FOUND(404, "Requested resource was not found"),
    AUTHENTICATION_FAILED(401, "Invalid credentials or unauthenticated request"),
    ACCESS_DENIED(403, "You do not have permission to perform this action"),
    VALIDATION_FAILED(400, "Validation failed on the submitted data"),
    DUPLICATE_ENTITY(409, "A record with the specified unique field already exists"),
    INVALID_STATE_TRANSITION(422, "The requested status transition is not permitted"),
    DATABASE_OPERATION_FAILED(500, "An error occurred during database persistence"),
    INTERNAL_SERVER_ERROR(500, "An unexpected server error occurred");

    private final int httpStatus;
    private final String defaultMessage;

    ErrorCode(int httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}

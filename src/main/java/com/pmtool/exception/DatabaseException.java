package com.pmtool.exception;

/**
 * Thrown when an underlying JDBC or relational SQL error occurs.
 */
public class DatabaseException extends AppException {

    public DatabaseException(String message) {
        super(ErrorCode.DATABASE_OPERATION_FAILED, message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(ErrorCode.DATABASE_OPERATION_FAILED, message, cause);
    }
}

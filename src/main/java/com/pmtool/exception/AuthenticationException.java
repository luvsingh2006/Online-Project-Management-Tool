package com.pmtool.exception;

/**
 * Thrown when credentials are invalid or a session is expired or revoked.
 */
public class AuthenticationException extends AppException {

    public AuthenticationException(String message) {
        super(ErrorCode.AUTHENTICATION_FAILED, message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(ErrorCode.AUTHENTICATION_FAILED, message, cause);
    }
}

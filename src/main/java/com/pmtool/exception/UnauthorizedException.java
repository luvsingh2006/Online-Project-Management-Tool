package com.pmtool.exception;

/**
 * Thrown when an authenticated user attempts an operation outside their authorized role.
 */
public class UnauthorizedException extends AppException {

    public UnauthorizedException(String message) {
        super(ErrorCode.ACCESS_DENIED, message);
    }

    public UnauthorizedException(String requiredRole, String currentRole) {
        super(ErrorCode.ACCESS_DENIED, String.format("Operation requires role '%s', but current role is '%s'", requiredRole, currentRole));
        addDetail("requiredRole", requiredRole);
        addDetail("currentRole", currentRole);
    }
}

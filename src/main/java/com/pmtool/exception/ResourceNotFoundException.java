package com.pmtool.exception;

/**
 * Thrown when an entity cannot be found by its identifier.
 */
public class ResourceNotFoundException extends AppException {

    public ResourceNotFoundException(String resourceName, Object identifier) {
        super(ErrorCode.RESOURCE_NOT_FOUND, String.format("%s with identifier '%s' was not found", resourceName, identifier));
        addDetail("resource", resourceName);
        addDetail("identifier", String.valueOf(identifier));
    }

    public ResourceNotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message);
    }
}

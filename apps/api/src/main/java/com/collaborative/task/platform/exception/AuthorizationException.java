package com.collaborative.task.platform.exception;

/**
 * Exception thrown when authorization fails
 * Used for insufficient permissions, access denied, etc.
 */
public class AuthorizationException extends RuntimeException {

    public AuthorizationException(String message) {
        super(message);
    }

    public AuthorizationException(String message, Throwable cause) {
        super(message, cause);
    }
}
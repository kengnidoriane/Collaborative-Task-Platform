package com.collaborative.task.platform.exception;

/**
 * Exception thrown when authentication fails
 * Used for invalid credentials, expired tokens, etc.
 */
public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
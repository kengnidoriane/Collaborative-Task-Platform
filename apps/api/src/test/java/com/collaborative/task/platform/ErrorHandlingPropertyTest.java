package com.collaborative.task.platform;

import com.collaborative.task.platform.exception.*;
import net.jqwik.api.*;
import net.jqwik.api.constraints.NotBlank;
import net.jqwik.api.constraints.NotEmpty;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for error handling functionality
 * **Feature: collaborative-task-platform, Property 26: Error handling is graceful and informative**
 * **Validates: Requirements 6.4**
 */
class ErrorHandlingPropertyTest {

    private GlobalExceptionHandler exceptionHandler;
    private WebRequest mockWebRequest;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        mockWebRequest = mock(WebRequest.class);
    }

    /**
     * Property: For any business exception, error handling should be graceful and informative
     * Tests that business exceptions are properly handled with RFC 7807 Problem Details
     */
    @Property(tries = 50)
    void businessExceptionHandlingIsGracefulAndInformative(
            @ForAll @NotBlank String message,
            @ForAll HttpStatus status,
            @ForAll @NotBlank String errorCode) {
        
        // Arrange
        BusinessException exception = new BusinessException(message, status, errorCode);
        
        // Act
        ResponseEntity<ProblemDetail> response = exceptionHandler.handleBusinessException(exception, mockWebRequest);
        
        // Assert - Error handling is graceful (no exceptions thrown)
        assertNotNull(response, "Response should not be null");
        assertNotNull(response.getBody(), "Response body should not be null");
        
        // Assert - Error handling is informative
        ProblemDetail problemDetail = response.getBody();
        assertEquals(status, response.getStatusCode(), "HTTP status should match exception status");
        assertEquals(message, problemDetail.getDetail(), "Error message should be preserved");
        assertEquals("Business Logic Error", problemDetail.getTitle(), "Title should be descriptive");
        assertNotNull(problemDetail.getType(), "Problem type should be set");
        assertNotNull(problemDetail.getProperties().get("timestamp"), "Timestamp should be included");
        assertEquals(errorCode, problemDetail.getProperties().get("errorCode"), "Error code should be included");
        
        // Assert - Timestamp is recent (within last minute)
        Instant timestamp = (Instant) problemDetail.getProperties().get("timestamp");
        assertTrue(timestamp.isAfter(Instant.now().minusSeconds(60)), "Timestamp should be recent");
    }

    /**
     * Property: For any authentication exception, error handling should be graceful and informative
     */
    @Property(tries = 50)
    void authenticationExceptionHandlingIsGracefulAndInformative(
            @ForAll @NotBlank String message) {
        
        // Arrange
        AuthenticationException exception = new AuthenticationException(message);
        
        // Act
        ResponseEntity<ProblemDetail> response = exceptionHandler.handleAuthenticationException(exception, mockWebRequest);
        
        // Assert - Error handling is graceful
        assertNotNull(response, "Response should not be null");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode(), "Should return 401 Unauthorized");
        
        // Assert - Error handling is informative
        ProblemDetail problemDetail = response.getBody();
        assertEquals(message, problemDetail.getDetail(), "Error message should be preserved");
        assertEquals("Authentication Error", problemDetail.getTitle(), "Title should be descriptive");
        assertNotNull(problemDetail.getType(), "Problem type should be set");
        assertNotNull(problemDetail.getProperties().get("timestamp"), "Timestamp should be included");
    }

    /**
     * Property: For any authorization exception, error handling should be graceful and informative
     */
    @Property(tries = 50)
    void authorizationExceptionHandlingIsGracefulAndInformative(
            @ForAll @NotBlank String message) {
        
        // Arrange
        AuthorizationException exception = new AuthorizationException(message);
        
        // Act
        ResponseEntity<ProblemDetail> response = exceptionHandler.handleAuthorizationException(exception, mockWebRequest);
        
        // Assert - Error handling is graceful
        assertNotNull(response, "Response should not be null");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), "Should return 403 Forbidden");
        
        // Assert - Error handling is informative
        ProblemDetail problemDetail = response.getBody();
        assertEquals(message, problemDetail.getDetail(), "Error message should be preserved");
        assertEquals("Authorization Error", problemDetail.getTitle(), "Title should be descriptive");
        assertNotNull(problemDetail.getType(), "Problem type should be set");
        assertNotNull(problemDetail.getProperties().get("timestamp"), "Timestamp should be included");
    }

    /**
     * Property: For any resource not found exception, error handling should be graceful and informative
     */
    @Property(tries = 100)
    void resourceNotFoundExceptionHandlingIsGracefulAndInformative(
            @ForAll @NotBlank String resourceType,
            @ForAll @NotBlank String resourceId) {
        
        // Arrange
        ResourceNotFoundException exception = new ResourceNotFoundException(resourceType, resourceId);
        
        // Act
        ResponseEntity<ProblemDetail> response = exceptionHandler.handleResourceNotFoundException(exception, mockWebRequest);
        
        // Assert - Error handling is graceful
        assertNotNull(response, "Response should not be null");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(), "Should return 404 Not Found");
        
        // Assert - Error handling is informative
        ProblemDetail problemDetail = response.getBody();
        assertTrue(problemDetail.getDetail().contains(resourceType), "Error message should contain resource type");
        assertTrue(problemDetail.getDetail().contains(resourceId), "Error message should contain resource ID");
        assertEquals("Resource Not Found", problemDetail.getTitle(), "Title should be descriptive");
        assertNotNull(problemDetail.getType(), "Problem type should be set");
        assertNotNull(problemDetail.getProperties().get("timestamp"), "Timestamp should be included");
        assertEquals(resourceType, problemDetail.getProperties().get("resourceType"), "Resource type should be included");
        assertEquals(resourceId, problemDetail.getProperties().get("resourceId"), "Resource ID should be included");
    }

    /**
     * Property: For any constraint violation exception, error handling should be graceful and informative
     */
    @Property(tries = 100)
    void constraintViolationExceptionHandlingIsGracefulAndInformative(
            @ForAll @NotEmpty Set<@NotBlank String> violationMessages) {
        
        // Arrange
        Set<ConstraintViolation<?>> violations = new HashSet<>();
        for (String message : violationMessages) {
            ConstraintViolation<?> violation = mock(ConstraintViolation.class);
            when(violation.getMessage()).thenReturn(message);
            when(violation.getPropertyPath()).thenReturn(mock(jakarta.validation.Path.class));
            when(violation.getPropertyPath().toString()).thenReturn("testField");
            violations.add(violation);
        }
        
        ConstraintViolationException exception = new ConstraintViolationException("Constraint violation", violations);
        
        // Act
        ResponseEntity<ProblemDetail> response = exceptionHandler.handleConstraintViolationException(exception, mockWebRequest);
        
        // Assert - Error handling is graceful
        assertNotNull(response, "Response should not be null");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "Should return 400 Bad Request");
        
        // Assert - Error handling is informative
        ProblemDetail problemDetail = response.getBody();
        assertEquals("Constraint violation", problemDetail.getDetail(), "Error message should be preserved");
        assertEquals("Constraint Violation", problemDetail.getTitle(), "Title should be descriptive");
        assertNotNull(problemDetail.getType(), "Problem type should be set");
        assertNotNull(problemDetail.getProperties().get("timestamp"), "Timestamp should be included");
        assertNotNull(problemDetail.getProperties().get("violations"), "Violations should be included");
        
        @SuppressWarnings("unchecked")
        Map<String, String> violationsMap = (Map<String, String>) problemDetail.getProperties().get("violations");
        assertFalse(violationsMap.isEmpty(), "Violations map should not be empty");
    }

    /**
     * Property: For any validation exception, error handling should be graceful and informative
     */
    @Property(tries = 100)
    void validationExceptionHandlingIsGracefulAndInformative(
            @ForAll @NotBlank String fieldName,
            @ForAll @NotBlank String errorMessage) {
        
        // Arrange
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("testObject", fieldName, errorMessage);
        
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(java.util.List.of(fieldError));
        
        // Act
        ResponseEntity<ProblemDetail> response = exceptionHandler.handleValidationException(exception, mockWebRequest);
        
        // Assert - Error handling is graceful
        assertNotNull(response, "Response should not be null");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "Should return 400 Bad Request");
        
        // Assert - Error handling is informative
        ProblemDetail problemDetail = response.getBody();
        assertEquals("Validation failed", problemDetail.getDetail(), "Error message should be descriptive");
        assertEquals("Validation Error", problemDetail.getTitle(), "Title should be descriptive");
        assertNotNull(problemDetail.getType(), "Problem type should be set");
        assertNotNull(problemDetail.getProperties().get("timestamp"), "Timestamp should be included");
        assertNotNull(problemDetail.getProperties().get("validationErrors"), "Validation errors should be included");
        
        @SuppressWarnings("unchecked")
        Map<String, String> validationErrors = (Map<String, String>) problemDetail.getProperties().get("validationErrors");
        assertTrue(validationErrors.containsKey(fieldName), "Field name should be in validation errors");
        assertEquals(errorMessage, validationErrors.get(fieldName), "Error message should match");
    }

    /**
     * Property: For any generic exception, error handling should be graceful and informative
     */
    @Property(tries = 100)
    void genericExceptionHandlingIsGracefulAndInformative(
            @ForAll @NotBlank String message) {
        
        // Arrange
        Exception exception = new RuntimeException(message);
        
        // Act
        ResponseEntity<ProblemDetail> response = exceptionHandler.handleGenericException(exception, mockWebRequest);
        
        // Assert - Error handling is graceful
        assertNotNull(response, "Response should not be null");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode(), "Should return 500 Internal Server Error");
        
        // Assert - Error handling is informative (but doesn't expose internal details)
        ProblemDetail problemDetail = response.getBody();
        assertEquals("An unexpected error occurred", problemDetail.getDetail(), "Should provide generic error message");
        assertEquals("Internal Server Error", problemDetail.getTitle(), "Title should be descriptive");
        assertNotNull(problemDetail.getType(), "Problem type should be set");
        assertNotNull(problemDetail.getProperties().get("timestamp"), "Timestamp should be included");
        
        // Assert - Internal details are not exposed for security
        assertFalse(problemDetail.getDetail().contains(message), "Internal exception message should not be exposed");
    }

    /**
     * Property: All error responses should follow RFC 7807 Problem Details format
     */
    @Property(tries = 100)
    void allErrorResponsesFollowRFC7807Format(
            @ForAll @NotBlank String message,
            @ForAll HttpStatus status,
            @ForAll @NotBlank String errorCode) {
        
        // Test with BusinessException as representative
        BusinessException exception = new BusinessException(message, status, errorCode);
        ResponseEntity<ProblemDetail> response = exceptionHandler.handleBusinessException(exception, mockWebRequest);
        
        ProblemDetail problemDetail = response.getBody();
        assertNotNull(problemDetail, "Problem detail should not be null");
        
        // RFC 7807 required fields
        assertNotNull(problemDetail.getType(), "Type URI should be present");
        assertNotNull(problemDetail.getTitle(), "Title should be present");
        assertNotNull(problemDetail.getDetail(), "Detail should be present");
        
        // Additional informative fields
        assertNotNull(problemDetail.getProperties().get("timestamp"), "Timestamp should be present for traceability");
        
        // Type should be a valid URI format
        String typeUri = problemDetail.getType().toString();
        assertTrue(typeUri.startsWith("https://"), "Type should be a valid HTTPS URI");
        assertTrue(typeUri.contains("problems/"), "Type should indicate it's a problem type");
    }

    // Arbitraries for generating test data
    @Provide
    Arbitrary<HttpStatus> httpStatus() {
        return Arbitraries.of(
            HttpStatus.BAD_REQUEST,
            HttpStatus.UNAUTHORIZED,
            HttpStatus.FORBIDDEN,
            HttpStatus.NOT_FOUND,
            HttpStatus.CONFLICT,
            HttpStatus.UNPROCESSABLE_ENTITY,
            HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}
package com.mmtext.authserver.exception;

import com.mmtext.authserver.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice

public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            WebRequest request) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponse errorResponse = new ErrorResponse(
                "Validation failed", HttpStatus.BAD_REQUEST.value(),
                "Bad Request", request.getDescription(false).replace("uri=", ""),
                Instant.now(), errors
        );


        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            WebRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
                ex.getMessage(), HttpStatus.NOT_FOUND.value(),
                "Not Found", request.getDescription(false).replace("uri=", ""),
                Instant.now(), null
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExistsException(
            UserAlreadyExistsException ex,
            WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                ex.getMessage(), HttpStatus.CONFLICT.value(),
                "Conflict", request.getDescription(false).replace("uri=", ""),
                Instant.now(), null
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler({InvalidCredentialsException.class, BadCredentialsException.class})
    public ResponseEntity<ErrorResponse> handleInvalidCredentialsException(
            Exception ex,
            WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                "Invalid credentials", HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized", request.getDescription(false).replace("uri=", ""),
                Instant.now(), null
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ErrorResponse> handleAccountLockedException(
            AccountLockedException ex,
            WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                ex.getMessage(), HttpStatus.LOCKED.value(),
                "Account Locked", request.getDescription(false).replace("uri=", ""),
                Instant.now(), null
        );

        return ResponseEntity.status(HttpStatus.LOCKED).body(errorResponse);
    }

    @ExceptionHandler({InvalidTokenException.class, TokenExpiredException.class})
    public ResponseEntity<ErrorResponse> handleTokenException(
            Exception ex,
            WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                ex.getMessage(), HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized", request.getDescription(false).replace("uri=", ""),
                Instant.now(), null
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(TokenReuseException.class)
    public ResponseEntity<ErrorResponse> handleTokenReuseException(
            TokenReuseException ex,
            WebRequest request) {

        log.error("Token reuse detected: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                ex.getMessage(), HttpStatus.UNAUTHORIZED.value(),
        "Security Violation", request.getDescription(false).replace("uri=", ""),
                Instant.now(), null
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex,
            WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                "Access denied", HttpStatus.FORBIDDEN.value(),
                "Forbidden", request.getDescription(false).replace("uri=", ""),
                Instant.now(), null
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex,
            WebRequest request) {

        log.error("Unexpected error occurred", ex);
        ErrorResponse errorResponse = new ErrorResponse(
                "An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error", request.getDescription(false).replace("uri=", ""),
                Instant.now(), null
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}

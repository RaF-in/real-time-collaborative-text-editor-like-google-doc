package com.mmtext.editorservershare.dto;

import java.time.Instant;
import java.util.Map;

public class ValidationErrorResponse {
    private int status;
    private String message;
    private Map<String, String> errors;
    private Instant timestamp;

    public ValidationErrorResponse() {
    }

    public ValidationErrorResponse(int status, String message, Map<String, String> errors, Instant timestamp) {
        this.status = status;
        this.message = message;
        this.errors = errors;
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    public void setErrors(Map<String, String> errors) {
        this.errors = errors;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}

package com.mmtext.authserver.dto;

import java.time.Instant;
import java.util.Map;

public class ErrorResponse {
    private String message;
    private int status;
    private String error;
    private String path;
    private Instant timestamp;
    private Map<String, String> validationErrors;

    public ErrorResponse() {
    }

    public ErrorResponse(String message, int status, String error, String path, Instant timestamp, Map<String, String> validationErrors) {
        this.message = message;
        this.status = status;
        this.error = error;
        this.path = path;
        this.timestamp = timestamp;
        this.validationErrors = validationErrors;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, String> getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(Map<String, String> validationErrors) {
        this.validationErrors = validationErrors;
    }
}
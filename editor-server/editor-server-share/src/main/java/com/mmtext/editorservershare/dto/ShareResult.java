package com.mmtext.editorservershare.dto;

public class ShareResult {
    private String email;
    private Boolean success;
    private String message;
    private DocumentPermissionResponse permission;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public DocumentPermissionResponse getPermission() {
        return permission;
    }

    public void setPermission(DocumentPermissionResponse permission) {
        this.permission = permission;
    }
}
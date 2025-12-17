package com.mmtext.editorservershare.dto;


import com.mmtext.editorservershare.enums.PermissionLevel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class RequestAccessDto {

    @NotNull(message = "Requested permission is required")
    private PermissionLevel requestedPermission;

    @Size(max = 500, message = "Message cannot exceed 500 characters")
    private String message;

    public PermissionLevel getRequestedPermission() {
        return requestedPermission;
    }

    public void setRequestedPermission(PermissionLevel requestedPermission) {
        this.requestedPermission = requestedPermission;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
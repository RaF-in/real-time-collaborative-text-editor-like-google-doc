package com.mmtext.editorservershared.dto;

import com.mmtext.editorservershared.enums.PermissionLevel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ResolveAccessRequestDto {

    @NotNull(message = "Approve flag is required")
    private Boolean approve;

    private PermissionLevel permissionLevel;

    @Size(max = 500, message = "Message cannot exceed 500 characters")
    private String message;

    public Boolean getApprove() {
        return approve;
    }

    public void setApprove(Boolean approve) {
        this.approve = approve;
    }

    public PermissionLevel getPermissionLevel() {
        return permissionLevel;
    }

    public void setPermissionLevel(PermissionLevel permissionLevel) {
        this.permissionLevel = permissionLevel;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

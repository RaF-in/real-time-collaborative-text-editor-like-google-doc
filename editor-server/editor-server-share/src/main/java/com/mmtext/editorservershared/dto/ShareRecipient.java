package com.mmtext.editorservershared.dto;


import com.mmtext.editorservershared.enums.PermissionLevel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Email;


public class ShareRecipient {

    @Email(message = "Invalid email format")
    @NotNull(message = "Email is required")
    private String email;

    @NotNull(message = "Permission level is required")
    private PermissionLevel permissionLevel;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public PermissionLevel getPermissionLevel() {
        return permissionLevel;
    }

    public void setPermissionLevel(PermissionLevel permissionLevel) {
        this.permissionLevel = permissionLevel;
    }
}

package com.mmtext.editorservershared.dto;


import com.mmtext.editorservershared.enums.PermissionLevel;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;


public class UpdatePermissionDto {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Permission level is required")
    private PermissionLevel permissionLevel;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public PermissionLevel getPermissionLevel() {
        return permissionLevel;
    }

    public void setPermissionLevel(PermissionLevel permissionLevel) {
        this.permissionLevel = permissionLevel;
    }
}
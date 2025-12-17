package com.mmtext.editorservershared.dto;

import com.mmtext.editorservershared.enums.PermissionLevel;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;


public class CreateShareableLinkDto {

    @NotNull(message = "Permission level is required")
    private PermissionLevel permissionLevel;

    @Min(value = 1, message = "Expiration days must be at least 1")
    private Integer expiresInDays;

    public PermissionLevel getPermissionLevel() {
        return permissionLevel;
    }

    public void setPermissionLevel(PermissionLevel permissionLevel) {
        this.permissionLevel = permissionLevel;
    }

    public Integer getExpiresInDays() {
        return expiresInDays;
    }

    public void setExpiresInDays(Integer expiresInDays) {
        this.expiresInDays = expiresInDays;
    }
}

package com.mmtext.editorservershare.dto;

import com.mmtext.editorservershare.enums.PermissionLevel;

import java.time.Instant;
import java.util.UUID;

public class DocumentPermissionResponse {
    private UUID id;
    private UUID userId;
    private String userEmail;
    private String userName;
    private String userAvatarUrl;
    private PermissionLevel permissionLevel;
    private Instant grantedAt;
    private Boolean canRemove;
    private Boolean canChangePermission;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserAvatarUrl() {
        return userAvatarUrl;
    }

    public void setUserAvatarUrl(String userAvatarUrl) {
        this.userAvatarUrl = userAvatarUrl;
    }

    public PermissionLevel getPermissionLevel() {
        return permissionLevel;
    }

    public void setPermissionLevel(PermissionLevel permissionLevel) {
        this.permissionLevel = permissionLevel;
    }

    public Instant getGrantedAt() {
        return grantedAt;
    }

    public void setGrantedAt(Instant grantedAt) {
        this.grantedAt = grantedAt;
    }

    public Boolean getCanRemove() {
        return canRemove;
    }

    public void setCanRemove(Boolean canRemove) {
        this.canRemove = canRemove;
    }

    public Boolean getCanChangePermission() {
        return canChangePermission;
    }

    public void setCanChangePermission(Boolean canChangePermission) {
        this.canChangePermission = canChangePermission;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private UUID userId;
        private String userEmail;
        private String userName;
        private String userAvatarUrl;
        private PermissionLevel permissionLevel;
        private Instant grantedAt;
        private Boolean canRemove;
        private Boolean canChangePermission;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder userId(UUID userId) {
            this.userId = userId;
            return this;
        }

        public Builder userEmail(String userEmail) {
            this.userEmail = userEmail;
            return this;
        }

        public Builder userName(String userName) {
            this.userName = userName;
            return this;
        }

        public Builder userAvatarUrl(String userAvatarUrl) {
            this.userAvatarUrl = userAvatarUrl;
            return this;
        }

        public Builder permissionLevel(PermissionLevel permissionLevel) {
            this.permissionLevel = permissionLevel;
            return this;
        }

        public Builder grantedAt(Instant grantedAt) {
            this.grantedAt = grantedAt;
            return this;
        }

        public Builder canRemove(Boolean canRemove) {
            this.canRemove = canRemove;
            return this;
        }

        public Builder canChangePermission(Boolean canChangePermission) {
            this.canChangePermission = canChangePermission;
            return this;
        }

        public DocumentPermissionResponse build() {
            DocumentPermissionResponse response = new DocumentPermissionResponse();
            response.id = this.id;
            response.userId = this.userId;
            response.userEmail = this.userEmail;
            response.userName = this.userName;
            response.userAvatarUrl = this.userAvatarUrl;
            response.permissionLevel = this.permissionLevel;
            response.grantedAt = this.grantedAt;
            response.canRemove = this.canRemove;
            response.canChangePermission = this.canChangePermission;
            return response;
        }
    }
}
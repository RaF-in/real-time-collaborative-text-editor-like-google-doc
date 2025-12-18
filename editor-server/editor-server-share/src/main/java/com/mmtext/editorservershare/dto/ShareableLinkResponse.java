package com.mmtext.editorservershare.dto;


import com.mmtext.editorservershare.enums.PermissionLevel;

import java.time.Instant;
import java.util.UUID;

public class ShareableLinkResponse {
    private UUID id;
    private String linkToken;
    private String fullUrl;
    private PermissionLevel permissionLevel;
    private Instant createdAt;
    private Instant expiresAt;
    private Boolean isActive;
    private Integer accessCount;
    private Instant lastAccessedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getLinkToken() {
        return linkToken;
    }

    public void setLinkToken(String linkToken) {
        this.linkToken = linkToken;
    }

    public String getFullUrl() {
        return fullUrl;
    }

    public void setFullUrl(String fullUrl) {
        this.fullUrl = fullUrl;
    }

    public PermissionLevel getPermissionLevel() {
        return permissionLevel;
    }

    public void setPermissionLevel(PermissionLevel permissionLevel) {
        this.permissionLevel = permissionLevel;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }

    public Integer getAccessCount() {
        return accessCount;
    }

    public void setAccessCount(Integer accessCount) {
        this.accessCount = accessCount;
    }

    public Instant getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void setLastAccessedAt(Instant lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private String linkToken;
        private String fullUrl;
        private PermissionLevel permissionLevel;
        private Instant createdAt;
        private Instant expiresAt;
        private Boolean isActive;
        private Integer accessCount;
        private Instant lastAccessedAt;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder linkToken(String linkToken) {
            this.linkToken = linkToken;
            return this;
        }

        public Builder fullUrl(String fullUrl) {
            this.fullUrl = fullUrl;
            return this;
        }

        public Builder permissionLevel(PermissionLevel permissionLevel) {
            this.permissionLevel = permissionLevel;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder expiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public Builder isActive(Boolean isActive) {
            this.isActive = isActive;
            return this;
        }

        public Builder accessCount(Integer accessCount) {
            this.accessCount = accessCount;
            return this;
        }

        public Builder lastAccessedAt(Instant lastAccessedAt) {
            this.lastAccessedAt = lastAccessedAt;
            return this;
        }

        public ShareableLinkResponse build() {
            ShareableLinkResponse response = new ShareableLinkResponse();
            response.id = this.id;
            response.linkToken = this.linkToken;
            response.fullUrl = this.fullUrl;
            response.permissionLevel = this.permissionLevel;
            response.createdAt = this.createdAt;
            response.expiresAt = this.expiresAt;
            response.isActive = this.isActive;
            response.accessCount = this.accessCount;
            response.lastAccessedAt = this.lastAccessedAt;
            return response;
        }
    }
}

package com.mmtext.editorservershare.dto;


import com.mmtext.editorservershare.enums.PermissionLevel;
import com.mmtext.editorservershare.enums.RequestStatus;

import java.time.Instant;
import java.util.UUID;

public class AccessRequestResponse {
    private UUID id;
    private UUID documentId;
    private String documentTitle;
    private UUID requesterId;
    private String requesterEmail;
    private String requesterName;
    private String requesterAvatarUrl;
    private PermissionLevel requestedPermission;
    private String message;
    private RequestStatus status;
    private Instant requestedAt;
    private Instant resolvedAt;
    private UUID resolvedBy;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public String getDocumentTitle() {
        return documentTitle;
    }

    public void setDocumentTitle(String documentTitle) {
        this.documentTitle = documentTitle;
    }

    public UUID getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(UUID requesterId) {
        this.requesterId = requesterId;
    }

    public String getRequesterEmail() {
        return requesterEmail;
    }

    public void setRequesterEmail(String requesterEmail) {
        this.requesterEmail = requesterEmail;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public void setRequesterName(String requesterName) {
        this.requesterName = requesterName;
    }

    public String getRequesterAvatarUrl() {
        return requesterAvatarUrl;
    }

    public void setRequesterAvatarUrl(String requesterAvatarUrl) {
        this.requesterAvatarUrl = requesterAvatarUrl;
    }

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

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public UUID getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(UUID resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    // Builder pattern implementation
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private UUID documentId;
        private String documentTitle;
        private UUID requesterId;
        private String requesterEmail;
        private String requesterName;
        private String requesterAvatarUrl;
        private PermissionLevel requestedPermission;
        private String message;
        private RequestStatus status;
        private Instant requestedAt;
        private Instant resolvedAt;
        private UUID resolvedBy;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder documentId(UUID documentId) {
            this.documentId = documentId;
            return this;
        }

        public Builder documentTitle(String documentTitle) {
            this.documentTitle = documentTitle;
            return this;
        }

        public Builder requesterId(UUID requesterId) {
            this.requesterId = requesterId;
            return this;
        }

        public Builder requesterEmail(String requesterEmail) {
            this.requesterEmail = requesterEmail;
            return this;
        }

        public Builder requesterName(String requesterName) {
            this.requesterName = requesterName;
            return this;
        }

        public Builder requesterAvatarUrl(String requesterAvatarUrl) {
            this.requesterAvatarUrl = requesterAvatarUrl;
            return this;
        }

        public Builder requestedPermission(PermissionLevel requestedPermission) {
            this.requestedPermission = requestedPermission;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder status(RequestStatus status) {
            this.status = status;
            return this;
        }

        public Builder requestedAt(Instant requestedAt) {
            this.requestedAt = requestedAt;
            return this;
        }

        public Builder resolvedAt(Instant resolvedAt) {
            this.resolvedAt = resolvedAt;
            return this;
        }

        public Builder resolvedBy(UUID resolvedBy) {
            this.resolvedBy = resolvedBy;
            return this;
        }

        public AccessRequestResponse build() {
            AccessRequestResponse response = new AccessRequestResponse();
            response.id = this.id;
            response.documentId = this.documentId;
            response.documentTitle = this.documentTitle;
            response.requesterId = this.requesterId;
            response.requesterEmail = this.requesterEmail;
            response.requesterName = this.requesterName;
            response.requesterAvatarUrl = this.requesterAvatarUrl;
            response.requestedPermission = this.requestedPermission;
            response.message = this.message;
            response.status = this.status;
            response.requestedAt = this.requestedAt;
            response.resolvedAt = this.resolvedAt;
            response.resolvedBy = this.resolvedBy;
            return response;
        }
    }
}
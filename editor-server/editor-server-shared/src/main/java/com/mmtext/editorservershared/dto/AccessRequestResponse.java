package com.mmtext.editorservershared.dto;


import com.mmtext.editorservershared.enums.PermissionLevel;
import com.mmtext.editorservershared.enums.RequestStatus;

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
}
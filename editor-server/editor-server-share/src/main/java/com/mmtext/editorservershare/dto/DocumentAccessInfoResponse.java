package com.mmtext.editorservershare.dto;

import com.mmtext.editorservershare.enums.PermissionLevel;

import java.util.UUID;


public class DocumentAccessInfoResponse {
    private UUID documentId;
    private String title;
    private PermissionLevel userPermission;
    private Boolean canEdit;
    private Boolean canShare;
    private Boolean canManagePermissions;
    private Boolean canRequestAccess;
    private UUID ownerId;
    private String ownerEmail;
    private String ownerName;

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public PermissionLevel getUserPermission() {
        return userPermission;
    }

    public void setUserPermission(PermissionLevel userPermission) {
        this.userPermission = userPermission;
    }

    public Boolean getCanEdit() {
        return canEdit;
    }

    public void setCanEdit(Boolean canEdit) {
        this.canEdit = canEdit;
    }

    public Boolean getCanShare() {
        return canShare;
    }

    public void setCanShare(Boolean canShare) {
        this.canShare = canShare;
    }

    public Boolean getCanManagePermissions() {
        return canManagePermissions;
    }

    public void setCanManagePermissions(Boolean canManagePermissions) {
        this.canManagePermissions = canManagePermissions;
    }

    public Boolean getCanRequestAccess() {
        return canRequestAccess;
    }

    public void setCanRequestAccess(Boolean canRequestAccess) {
        this.canRequestAccess = canRequestAccess;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID documentId;
        private String title;
        private PermissionLevel userPermission;
        private Boolean canEdit;
        private Boolean canShare;
        private Boolean canManagePermissions;
        private Boolean canRequestAccess;
        private UUID ownerId;
        private String ownerEmail;
        private String ownerName;

        public Builder documentId(UUID documentId) {
            this.documentId = documentId;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder userPermission(PermissionLevel userPermission) {
            this.userPermission = userPermission;
            return this;
        }

        public Builder canEdit(Boolean canEdit) {
            this.canEdit = canEdit;
            return this;
        }

        public Builder canShare(Boolean canShare) {
            this.canShare = canShare;
            return this;
        }

        public Builder canManagePermissions(Boolean canManagePermissions) {
            this.canManagePermissions = canManagePermissions;
            return this;
        }

        public Builder canRequestAccess(Boolean canRequestAccess) {
            this.canRequestAccess = canRequestAccess;
            return this;
        }

        public Builder ownerId(UUID ownerId) {
            this.ownerId = ownerId;
            return this;
        }

        public Builder ownerEmail(String ownerEmail) {
            this.ownerEmail = ownerEmail;
            return this;
        }

        public Builder ownerName(String ownerName) {
            this.ownerName = ownerName;
            return this;
        }

        public DocumentAccessInfoResponse build() {
            DocumentAccessInfoResponse response = new DocumentAccessInfoResponse();
            response.documentId = this.documentId;
            response.title = this.title;
            response.userPermission = this.userPermission;
            response.canEdit = this.canEdit;
            response.canShare = this.canShare;
            response.canManagePermissions = this.canManagePermissions;
            response.canRequestAccess = this.canRequestAccess;
            response.ownerId = this.ownerId;
            response.ownerEmail = this.ownerEmail;
            response.ownerName = this.ownerName;
            return response;
        }
    }
}
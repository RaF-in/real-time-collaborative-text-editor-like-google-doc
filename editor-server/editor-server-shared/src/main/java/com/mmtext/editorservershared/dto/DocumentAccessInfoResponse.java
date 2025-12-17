package com.mmtext.editorservershared.dto;

import com.mmtext.editorservershared.enums.PermissionLevel;

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
}
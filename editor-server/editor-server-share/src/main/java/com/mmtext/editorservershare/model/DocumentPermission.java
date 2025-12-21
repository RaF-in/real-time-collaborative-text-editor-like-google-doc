package com.mmtext.editorservershare.model;

import com.mmtext.editorservershare.enums.PermissionLevel;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_permissions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"document_id", "user_id"}),
        indexes = {
                @Index(name = "idx_doc_permissions_document", columnList = "document_id"),
                @Index(name = "idx_doc_permissions_user", columnList = "user_id"),
                @Index(name = "idx_doc_permissions_lookup", columnList = "document_id, user_id")
        })

public class DocumentPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "document_id", nullable = false, length = 255)
    private String documentId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission_level", nullable = false, length = 20)
    private PermissionLevel permissionLevel;

    @Column(name = "granted_by")
    private UUID grantedBy;

    @CreationTimestamp
    @Column(name = "granted_at", nullable = false, updatable = false)
    private Instant grantedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Transient fields for joined data
    @Transient
    private String userEmail;

    @Transient
    private String userName;

    @Transient
    private String userAvatarUrl;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

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

    public UUID getGrantedBy() {
        return grantedBy;
    }

    public void setGrantedBy(UUID grantedBy) {
        this.grantedBy = grantedBy;
    }

    public Instant getGrantedAt() {
        return grantedAt;
    }

    public void setGrantedAt(Instant grantedAt) {
        this.grantedAt = grantedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
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

    // Builder pattern implementation
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String documentId;
        private UUID userId;
        private String userEmail;
        private String userName;
        private String userAvatarUrl;
        private PermissionLevel permissionLevel;
        private UUID grantedBy;

        public Builder documentId(String documentId) {
            this.documentId = documentId;
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

        public Builder grantedBy(UUID grantedBy) {
            this.grantedBy = grantedBy;
            return this;
        }

        public DocumentPermission build() {
            DocumentPermission permission = new DocumentPermission();
            permission.documentId = this.documentId;
            permission.userId = this.userId;
            permission.userEmail = this.userEmail;
            permission.userName = this.userName;
            permission.userAvatarUrl = this.userAvatarUrl;
            permission.permissionLevel = this.permissionLevel;
            permission.grantedBy = this.grantedBy;
            return permission;
        }
    }
}
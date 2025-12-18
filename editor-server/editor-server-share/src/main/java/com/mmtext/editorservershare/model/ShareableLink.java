package com.mmtext.editorservershare.model;
import com.mmtext.editorservershare.enums.PermissionLevel;
import jakarta.persistence.*;

import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "shareable_links",
        indexes = {
                @Index(name = "idx_shareable_links_token", columnList = "link_token", unique = true),
                @Index(name = "idx_shareable_links_document", columnList = "document_id"),
                @Index(name = "idx_shareable_links_active", columnList = "is_active, expires_at")
        })

public class ShareableLink {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "link_token", unique = true, nullable = false, length = 64)
    private String linkToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission_level", nullable = false, length = 20)
    private PermissionLevel permissionLevel;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "is_active", nullable = false)

    private Boolean isActive = true;

    @Column(name = "access_count", nullable = false)

    private Integer accessCount = 0;

    @Column(name = "last_accessed_at")
    private Instant lastAccessedAt;

    public boolean isValid() {
        if (!isActive) return false;
        if (expiresAt != null && Instant.now().isAfter(expiresAt)) {
            return false;
        }
        return true;
    }

    public void incrementAccessCount() {
        this.accessCount++;
        this.lastAccessedAt = Instant.now();
    }

    public void deactivate() {
        this.isActive = false;
    }

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

    public String getLinkToken() {
        return linkToken;
    }

    public void setLinkToken(String linkToken) {
        this.linkToken = linkToken;
    }

    public PermissionLevel getPermissionLevel() {
        return permissionLevel;
    }

    public void setPermissionLevel(PermissionLevel permissionLevel) {
        this.permissionLevel = permissionLevel;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public static Builder builder() {
        return new Builder();
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

    public static class Builder {
        private UUID id;
        private UUID documentId;
        private String linkToken;
        private PermissionLevel permissionLevel;
        private UUID createdBy;
        private Instant createdAt;
        private Instant expiresAt;
        private Boolean isActive = true;
        private Integer accessCount = 0;
        private Instant lastAccessedAt;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder documentId(UUID documentId) {
            this.documentId = documentId;
            return this;
        }

        public Builder linkToken(String linkToken) {
            this.linkToken = linkToken;
            return this;
        }

        public Builder permissionLevel(PermissionLevel permissionLevel) {
            this.permissionLevel = permissionLevel;
            return this;
        }

        public Builder createdBy(UUID createdBy) {
            this.createdBy = createdBy;
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

        public ShareableLink build() {
            ShareableLink link = new ShareableLink();
            link.id = this.id;
            link.documentId = this.documentId;
            link.linkToken = this.linkToken;
            link.permissionLevel = this.permissionLevel;
            link.createdBy = this.createdBy;
            link.createdAt = this.createdAt;
            link.expiresAt = this.expiresAt;
            link.isActive = this.isActive;
            link.accessCount = this.accessCount;
            link.lastAccessedAt = this.lastAccessedAt;
            return link;
        }
    }
}
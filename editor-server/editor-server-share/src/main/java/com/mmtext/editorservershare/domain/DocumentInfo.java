package com.mmtext.editorservershare.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Document basic info for sharing and access requests
 */
public class DocumentInfo {
    private String id;
    private String docId;
    private String title;
    private String ownerId;
    private boolean allowAccessRequests;
    private Instant createdAt;
    private Instant updatedAt;

    // Default constructor
    public DocumentInfo() {
    }

    // All-args constructor
    public DocumentInfo(String id, String docId, String title, String ownerId, boolean allowAccessRequests, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.docId = docId;
        this.title = title;
        this.ownerId = ownerId;
        this.allowAccessRequests = allowAccessRequests;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Builder pattern implementation
    public static Builder builder() {
        return new Builder();
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public boolean isAllowAccessRequests() {
        return allowAccessRequests;
    }

    public void setAllowAccessRequests(boolean allowAccessRequests) {
        this.allowAccessRequests = allowAccessRequests;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    // equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DocumentInfo that = (DocumentInfo) o;
        return allowAccessRequests == that.allowAccessRequests &&
                Objects.equals(id, that.id) &&
                Objects.equals(docId, that.docId) &&
                Objects.equals(title, that.title) &&
                Objects.equals(ownerId, that.ownerId) &&
                Objects.equals(createdAt, that.createdAt) &&
                Objects.equals(updatedAt, that.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, docId, title, ownerId, allowAccessRequests, createdAt, updatedAt);
    }

    // toString
    @Override
    public String toString() {
        return "DocumentInfo{" +
                "id='" + id + '\'' +
                ", docId='" + docId + '\'' +
                ", title='" + title + '\'' +
                ", ownerId='" + ownerId + '\'' +
                ", allowAccessRequests=" + allowAccessRequests +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }

    // Builder class
    public static class Builder {
        private String id;
        private String docId;
        private String title;
        private String ownerId;
        private boolean allowAccessRequests;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder docId(String docId) {
            this.docId = docId;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder ownerId(String ownerId) {
            this.ownerId = ownerId;
            return this;
        }

        public Builder allowAccessRequests(boolean allowAccessRequests) {
            this.allowAccessRequests = allowAccessRequests;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public DocumentInfo build() {
            return new DocumentInfo(id, docId, title, ownerId, allowAccessRequests, createdAt, updatedAt);
        }
    }
}
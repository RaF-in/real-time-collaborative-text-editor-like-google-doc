package com.mmtext.editorservershare.domain;

import java.time.Instant;
import java.util.UUID;
import java.util.Objects;

/**
 * Document domain model
 */
public class Document {
    private UUID id;
    private String docId;
    private String title;
    private UUID ownerId;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean allowAccessRequests;

    // Default constructor
    public Document() {
    }

    // All-args constructor
    public Document(UUID id, String docId, String title, UUID ownerId, Instant createdAt, Instant updatedAt, boolean allowAccessRequests) {
        this.id = id;
        this.docId = docId;
        this.title = title;
        this.ownerId = ownerId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.allowAccessRequests = allowAccessRequests;
    }

    // Builder pattern implementation
    public static Builder builder() {
        return new Builder();
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
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

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
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

    public boolean isAllowAccessRequests() {
        return allowAccessRequests;
    }

    public void setAllowAccessRequests(boolean allowAccessRequests) {
        this.allowAccessRequests = allowAccessRequests;
    }

    // equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Document document = (Document) o;
        return allowAccessRequests == document.allowAccessRequests &&
                Objects.equals(id, document.id) &&
                Objects.equals(docId, document.docId) &&
                Objects.equals(title, document.title) &&
                Objects.equals(ownerId, document.ownerId) &&
                Objects.equals(createdAt, document.createdAt) &&
                Objects.equals(updatedAt, document.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, docId, title, ownerId, createdAt, updatedAt, allowAccessRequests);
    }

    // toString
    @Override
    public String toString() {
        return "Document{" +
                "id=" + id +
                ", docId='" + docId + '\'' +
                ", title='" + title + '\'' +
                ", ownerId=" + ownerId +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", allowAccessRequests=" + allowAccessRequests +
                '}';
    }

    // Builder class
    public static class Builder {
        private UUID id;
        private String docId;
        private String title;
        private UUID ownerId;
        private Instant createdAt;
        private Instant updatedAt;
        private boolean allowAccessRequests;

        public Builder id(UUID id) {
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

        public Builder ownerId(UUID ownerId) {
            this.ownerId = ownerId;
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

        public Builder allowAccessRequests(boolean allowAccessRequests) {
            this.allowAccessRequests = allowAccessRequests;
            return this;
        }

        public Document build() {
            return new Document(id, docId, title, ownerId, createdAt, updatedAt, allowAccessRequests);
        }
    }
}
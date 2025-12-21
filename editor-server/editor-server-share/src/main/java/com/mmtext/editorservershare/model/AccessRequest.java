package com.mmtext.editorservershare.model;
import com.mmtext.editorservershare.enums.PermissionLevel;
import com.mmtext.editorservershare.enums.RequestStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "access_requests",
        indexes = {
                @Index(name = "idx_access_requests_document", columnList = "document_id"),
                @Index(name = "idx_access_requests_requester", columnList = "requester_id"),
                @Index(name = "idx_access_requests_status", columnList = "status"),
                @Index(name = "idx_access_requests_pending", columnList = "document_id, status")
        })

public class AccessRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "document_id", nullable = false, length = 255)
    private String documentId;

    @Column(name = "requester_id", nullable = false)
    private UUID requesterId;

    @Column(name = "requester_email", nullable = false)
    private String requesterEmail;

    @Column(name = "requester_name")
    private String requesterName;

    @Enumerated(EnumType.STRING)
    @Column(name = "requested_permission", nullable = false, length = 20)
    private PermissionLevel requestedPermission;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RequestStatus status = RequestStatus.PENDING;

    @CreationTimestamp
    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolved_by")
    private String resolvedBy;

    // Transient fields
    @Transient
    private String documentTitle;

    @Transient
    private UUID documentOwnerId;

    @Transient
    private String documentOwnerEmail;

    @Transient
    private String documentOwnerName;


    public void approve(String resolvedBy) {
        this.status = RequestStatus.APPROVED;
        this.resolvedAt = Instant.now();
        this.resolvedBy = resolvedBy;
    }

    public void reject(String resolvedBy) {
        this.status = RequestStatus.REJECTED;
        this.resolvedAt = Instant.now();
        this.resolvedBy = resolvedBy;
    }

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

    public String getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(String resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public String getDocumentTitle() {
        return documentTitle;
    }

    public void setDocumentTitle(String documentTitle) {
        this.documentTitle = documentTitle;
    }

    public UUID getDocumentOwnerId() {
        return documentOwnerId;
    }

    public void setDocumentOwnerId(UUID documentOwnerId) {
        this.documentOwnerId = documentOwnerId;
    }

    public String getDocumentOwnerEmail() {
        return documentOwnerEmail;
    }

    public void setDocumentOwnerEmail(String documentOwnerEmail) {
        this.documentOwnerEmail = documentOwnerEmail;
    }

    public String getDocumentOwnerName() {
        return documentOwnerName;
    }

    public void setDocumentOwnerName(String documentOwnerName) {
        this.documentOwnerName = documentOwnerName;
    }

    // Builder pattern implementation
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String documentId;
        private UUID requesterId;
        private String requesterEmail;
        private String requesterName;
        private PermissionLevel requestedPermission;
        private String message;
        private RequestStatus status = RequestStatus.PENDING;

        public Builder documentId(String documentId) {
            this.documentId = documentId;
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

        public AccessRequest build() {
            AccessRequest accessRequest = new AccessRequest();
            accessRequest.documentId = this.documentId;
            accessRequest.requesterId = this.requesterId;
            accessRequest.requesterEmail = this.requesterEmail;
            accessRequest.requesterName = this.requesterName;
            accessRequest.requestedPermission = this.requestedPermission;
            accessRequest.message = this.message;
            accessRequest.status = this.status != null ? this.status : RequestStatus.PENDING;
            return accessRequest;
        }
    }
}
package com.mmtext.editorservershare.model;

import com.mmtext.editorservershare.enums.PermissionLevel;
import com.mmtext.editorservershare.util.PostgreSQLInetType;
import jakarta.persistence.*;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "share_audit_logs",
        indexes = {
                @Index(name = "idx_audit_logs_document", columnList = "document_id"),
                @Index(name = "idx_audit_logs_user", columnList = "user_id"),
                @Index(name = "idx_audit_logs_created", columnList = "created_at"),
                @Index(name = "idx_audit_logs_action", columnList = "action")
        })

public class ShareAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "document_id", nullable = false, length = 255)
    private String documentId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "target_user_id")
    private UUID targetUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_permission", length = 20)
    private PermissionLevel oldPermission;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_permission", length = 20)
    private PermissionLevel newPermission;

    @Column(name = "ip_address", columnDefinition = "inet")
    @Type(PostgreSQLInetType.class)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")

    private Map<String, Object> metadata = new HashMap<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Audit actions
    public static final String ACTION_SHARE = "SHARE";
    public static final String ACTION_PERMISSION_CHANGE = "PERMISSION_CHANGE";
    public static final String ACTION_PERMISSION_REMOVE = "PERMISSION_REMOVE";
    public static final String ACTION_ACCESS_REQUEST = "ACCESS_REQUEST";
    public static final String ACTION_ACCESS_APPROVE = "ACCESS_APPROVE";
    public static final String ACTION_ACCESS_REJECT = "ACCESS_REJECT";
    public static final String ACTION_LINK_CREATE = "LINK_CREATE";
    public static final String ACTION_LINK_REVOKE = "LINK_REVOKE";
    public static final String ACTION_LINK_ACCESS = "LINK_ACCESS";

    public ShareAuditLog() {
    }

    public ShareAuditLog(String documentId, UUID userId, String action, UUID targetUserId, PermissionLevel oldPermission, PermissionLevel newPermission, String ipAddress, String userAgent, Map<String, Object> metadata, Instant createdAt) {
        this.documentId = documentId;
        this.userId = userId;
        this.action = action;
        this.targetUserId = targetUserId;
        this.oldPermission = oldPermission;
        this.newPermission = newPermission;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.metadata = metadata;
        this.createdAt = createdAt;
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

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public UUID getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(UUID targetUserId) {
        this.targetUserId = targetUserId;
    }

    public PermissionLevel getOldPermission() {
        return oldPermission;
    }

    public void setOldPermission(PermissionLevel oldPermission) {
        this.oldPermission = oldPermission;
    }

    public PermissionLevel getNewPermission() {
        return newPermission;
    }

    public void setNewPermission(PermissionLevel newPermission) {
        this.newPermission = newPermission;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
package com.mmtext.editorservershare.service;
import com.mmtext.editorservershare.enums.PermissionLevel;
import com.mmtext.editorservershare.model.ShareAuditLog;
import com.mmtext.editorservershare.repo.ShareAuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AuditService {

    private final ShareAuditLogRepository auditLogRepository;

    public AuditService(ShareAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Async
    public void logShare(
            String documentId,
            UUID userId,
            UUID targetUserId,
            PermissionLevel permissionLevel,
            HttpServletRequest request) {
        ShareAuditLog log = new ShareAuditLog(documentId, userId, ShareAuditLog.ACTION_SHARE, targetUserId, null,
                permissionLevel, getClientIp(request), request.getHeader("User-Agent"), null, Instant.now());

        auditLogRepository.save(log);
    }

    @Async
    public void logPermissionChange(
            String documentId,
            UUID userId,
            UUID targetUserId,
            PermissionLevel oldPermission,
            PermissionLevel newPermission,
            HttpServletRequest request) {
        ShareAuditLog log = new ShareAuditLog(documentId, userId, ShareAuditLog.ACTION_PERMISSION_CHANGE, targetUserId, oldPermission,
                newPermission, getClientIp(request), request.getHeader("User-Agent"), null, Instant.now());

        auditLogRepository.save(log);
    }

    @Async
    public void logPermissionRemove(
            String documentId,
            UUID userId,
            UUID targetUserId,
            PermissionLevel oldPermission,
            HttpServletRequest request) {
        ShareAuditLog log = new ShareAuditLog(documentId, userId, ShareAuditLog.ACTION_PERMISSION_REMOVE, targetUserId, oldPermission,
                null, getClientIp(request), request.getHeader("User-Agent"), null, Instant.now());

        auditLogRepository.save(log);
    }

    @Async
    public void logPermissionCreated(
            String documentId,
            UUID userId,
            PermissionLevel permissionLevel,
            String details) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("details", details);

        ShareAuditLog log = new ShareAuditLog(documentId, userId, ShareAuditLog.ACTION_PERMISSION_CHANGE, userId, null,
                permissionLevel, null, "ShareService-gRPC", metadata, Instant.now());

        auditLogRepository.save(log);
    }

    @Async
    public void logAccessRequest(
            String documentId,
            UUID userId,
            PermissionLevel requestedPermission,
            HttpServletRequest request) {

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("requestedPermission", requestedPermission.name());
        ShareAuditLog log = new ShareAuditLog(documentId, userId, ShareAuditLog.ACTION_ACCESS_REQUEST, null, null,
                null, getClientIp(request), request.getHeader("User-Agent"), null, Instant.now());

        auditLogRepository.save(log);
    }

    @Async
    public void logAccessApprove(
            String documentId,
            UUID userId,
            UUID targetUserId,
            PermissionLevel grantedPermission,
            HttpServletRequest request) {
        ShareAuditLog log = new ShareAuditLog(documentId, userId, ShareAuditLog.ACTION_ACCESS_APPROVE, targetUserId, null,
                grantedPermission, getClientIp(request), request.getHeader("User-Agent"), null, Instant.now());

        auditLogRepository.save(log);
    }

    @Async
    public void logAccessReject(
            String documentId,
            UUID userId,
            UUID targetUserId,
            HttpServletRequest request) {
        ShareAuditLog log = new ShareAuditLog(documentId, userId, ShareAuditLog.ACTION_ACCESS_REJECT, targetUserId, null,
                null, getClientIp(request), request.getHeader("User-Agent"), null, Instant.now());

        auditLogRepository.save(log);
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        if (ip == null || ip.isEmpty()) {
            return null;
        }

        // Handle multiple IPs in X-Forwarded-For header
        if (ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        // Validate IP format - return null if not a valid IP
        if (isValidIpAddress(ip)) {
            return ip;
        }

        return null;
    }

    private boolean isValidIpAddress(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }

        // IPv4 regex
        if (ip.matches("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$")) {
            return true;
        }

        // IPv6 regex (simplified)
        if (ip.matches("^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$") ||
            ip.contains(":")) {
            return true;
        }

        return false;
    }
}
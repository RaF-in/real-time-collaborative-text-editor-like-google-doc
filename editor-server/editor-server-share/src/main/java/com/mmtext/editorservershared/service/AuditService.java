package com.mmtext.editorservershared.service;
import com.mmtext.editorservershared.enums.PermissionLevel;
import com.mmtext.editorservershared.model.ShareAuditLog;
import com.mmtext.editorservershared.repo.ShareAuditLogRepository;
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
            UUID documentId,
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
            UUID documentId,
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
            UUID documentId,
            UUID userId,
            UUID targetUserId,
            PermissionLevel oldPermission,
            HttpServletRequest request) {
        ShareAuditLog log = new ShareAuditLog(documentId, userId, ShareAuditLog.ACTION_PERMISSION_REMOVE, targetUserId, oldPermission,
                null, getClientIp(request), request.getHeader("User-Agent"), null, Instant.now());

        auditLogRepository.save(log);
    }

    @Async
    public void logAccessRequest(
            UUID documentId,
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
            UUID documentId,
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
            UUID documentId,
            UUID userId,
            UUID targetUserId,
            HttpServletRequest request) {
        ShareAuditLog log = new ShareAuditLog(documentId, userId, ShareAuditLog.ACTION_ACCESS_REJECT, targetUserId, null,
                null, getClientIp(request), request.getHeader("User-Agent"), null, Instant.now());

        auditLogRepository.save(log);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip != null && ip.contains(",") ? ip.split(",")[0].trim() : ip;
    }
}
package com.mmtext.authserver.service;

import com.mmtext.authserver.enums.RiskLevel;
import com.mmtext.authserver.model.AuditLog;
import com.mmtext.authserver.repo.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void logAuthEvent(String eventType, UUID userId, String username,
                             String ipAddress, String userAgent, boolean success) {
        RiskLevel riskLevel = determineRiskLevel(eventType, success);

        AuditLog auditLog = new AuditLog(
                success, riskLevel, null, Instant.now(), userAgent, ipAddress,
                username, userId, eventType
        );

        auditLogRepository.save(auditLog);

        if (riskLevel == RiskLevel.HIGH || riskLevel == RiskLevel.CRITICAL) {
            log.warn("High risk auth event: {} for user: {} from IP: {}",
                    eventType, username, ipAddress);
        }
    }

    @Transactional
    public void logSecurityIncident(String eventType, UUID userId,
                                    String ipAddress, String userAgent, String metadata) {
        AuditLog auditLog = new AuditLog(
                false, RiskLevel.CRITICAL, null, Instant.now(), userAgent, ipAddress,
                null, userId, eventType
        );

        auditLogRepository.save(auditLog);

        log.error("SECURITY INCIDENT: {} for user: {} from IP: {} - {}",
                eventType, userId, ipAddress, metadata);
    }

    private RiskLevel determineRiskLevel(String eventType, boolean success) {
        if (!success) {
            return switch (eventType) {
                case "TOKEN_REUSE_DETECTED" -> RiskLevel.CRITICAL;
                case "LOGIN_FAILED_LOCKED" -> RiskLevel.HIGH;
                case "LOGIN_FAILED" -> RiskLevel.MEDIUM;
                default -> RiskLevel.LOW;
            };
        }

        return switch (eventType) {
            case "PASSWORD_CHANGED", "ACCOUNT_DELETED" -> RiskLevel.MEDIUM;
            default -> RiskLevel.LOW;
        };
    }
}
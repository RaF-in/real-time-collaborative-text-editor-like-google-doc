package com.mmtext.authserver.service;

import com.mmtext.authserver.config.JwtProperties;
import com.mmtext.authserver.exception.InvalidTokenException;
import com.mmtext.authserver.exception.TokenExpiredException;
import com.mmtext.authserver.exception.TokenReuseException;
import com.mmtext.authserver.model.RefreshToken;
import com.mmtext.authserver.repo.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final AuditService auditService;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtProperties jwtProperties, AuditService auditService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProperties = jwtProperties;
        this.auditService = auditService;
    }

    public String createRefreshToken(UUID userId, String deviceInfo, String ipAddress, String userAgent) {
        String token = UUID.randomUUID().toString();
        RefreshToken refreshToken = new RefreshToken(
                token, userId, Instant.now(), Instant.now().plus(jwtProperties.getRefreshToken().getExpirationDays(), ChronoUnit.DAYS),
                false, null, deviceInfo, ipAddress, userAgent
        );


        refreshTokenRepository.save(refreshToken);

        log.debug("Created refresh token for user: {} from IP: {}", userId, ipAddress);
        return token;
    }

    public Optional<RefreshToken> findValidToken(String token) {
        return refreshTokenRepository.findByTokenAndRevokedFalse(token)
                .filter(rt -> !rt.isExpired());
    }

    @Transactional
    public RefreshToken rotateToken(String oldTokenString, String deviceInfo, String ipAddress, String userAgent) {
        // Find the old token (even if revoked - for reuse detection)
        Optional<RefreshToken> oldTokenOpt = refreshTokenRepository.findByToken(oldTokenString);

        if (oldTokenOpt.isEmpty()) {
            log.warn("Refresh token not found: {}", oldTokenString);
            throw new InvalidTokenException("Invalid refresh token");
        }

        RefreshToken oldToken = oldTokenOpt.get();

        // CRITICAL: Reuse detection
        if (oldToken.getRevoked()) {
            log.error("Refresh token reuse detected for user: {} from IP: {}",
                    oldToken.getUserId(), ipAddress);

            // Revoke all tokens for this user (security incident)
            revokeAllForUser(oldToken.getUserId());

            // Log security incident
            auditService.logSecurityIncident(
                    "TOKEN_REUSE_DETECTED",
                    oldToken.getUserId(),
                    ipAddress,
                    userAgent,
                    "Refresh token reuse detected - all tokens revoked"
            );

            throw new TokenReuseException("Token reuse detected - all sessions revoked");
        }

        // Check if expired
        if (oldToken.isExpired()) {
            log.warn("Expired refresh token used for user: {}", oldToken.getUserId());
            throw new TokenExpiredException("Refresh token has expired");
        }

        // Create new token
        String newTokenString = UUID.randomUUID().toString();

        RefreshToken newToken = new RefreshToken(
                newTokenString, oldToken.getUserId(), Instant.now(), Instant.now().plus(jwtProperties.getRefreshToken().getExpirationDays(), ChronoUnit.DAYS),
                true, newTokenString, deviceInfo, ipAddress, userAgent
        );


        // Mark old token as revoked and link to new token
        oldToken.setRevoked(true);
        oldToken.setReplacedBy(newTokenString);

        refreshTokenRepository.save(oldToken);
        refreshTokenRepository.save(newToken);

        log.debug("Rotated refresh token for user: {}", oldToken.getUserId());
        return newToken;
    }

    @Transactional
    public void revokeToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
            log.debug("Revoked refresh token for user: {}", rt.getUserId());
        });
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        int revokedCount = refreshTokenRepository.revokeAllByUserId(userId);
        log.warn("Revoked {} refresh tokens for user: {}", revokedCount, userId);
    }

    public List<RefreshToken> findActiveTokensForUser(UUID userId) {
        return refreshTokenRepository.findByUserIdAndRevokedFalse(userId);
    }

    @Transactional
    public void cleanupExpiredTokens() {
        Instant threshold = Instant.now().minus(7, ChronoUnit.DAYS);
        List<RefreshToken> expiredTokens = refreshTokenRepository.findByExpiresAtBefore(threshold);

        if (!expiredTokens.isEmpty()) {
            refreshTokenRepository.deleteAll(expiredTokens);
            log.info("Cleaned up {} expired refresh tokens", expiredTokens.size());
        }
    }
}

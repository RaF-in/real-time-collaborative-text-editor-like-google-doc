package com.mmtext.authserver.service;

import com.mmtext.authserver.config.AppConfig;
import com.mmtext.authserver.dto.AuthResponse;
import com.mmtext.authserver.dto.LoginRequest;
import com.mmtext.authserver.dto.RefreshResponse;
import com.mmtext.authserver.dto.SignupRequest;
import com.mmtext.authserver.exception.InvalidCredentialsException;
import com.mmtext.authserver.exception.AccountLockedException;
import com.mmtext.authserver.model.User;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Service

public class AuthService {

    private final UserService userService;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final AppConfig appConfig;

    public AuthService(UserService userService, TokenService tokenService, RefreshTokenService refreshTokenService, PasswordEncoder passwordEncoder, AuditService auditService, AppConfig appConfig) {
        this.userService = userService;
        this.tokenService = tokenService;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.appConfig = appConfig;
    }

    @Transactional
    public AuthResponse signup(SignupRequest request, HttpServletRequest httpRequest) {
        User user = userService.createUser(
                request.getUsername(),
                request.getEmail(),
                request.getPassword()
        );

        String accessToken = tokenService.createAccessToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(
                user.getId(),
                getDeviceInfo(httpRequest),
                getClientIP(httpRequest),
                httpRequest.getHeader("User-Agent")
        );

        auditService.logAuthEvent(
                "SIGNUP_SUCCESS",
                user.getId(),
                user.getUsername(),
                getClientIP(httpRequest),
                httpRequest.getHeader("User-Agent"),
                true
        );

        return new AuthResponse(
                accessToken, "Bearer", tokenService.getAccessTokenExpirationSeconds(),
                userService.toUserInfo(user), refreshToken
        );
    }

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        User user;
        try {
            user = userService.findByUsername(request.getUsernameOrEmail());
        } catch (Exception e) {
            try {
                user = userService.findByEmail(request.getUsernameOrEmail());
            } catch (Exception ex) {
                auditService.logAuthEvent(
                        "LOGIN_FAILED",
                        null,
                        request.getUsernameOrEmail(),
                        getClientIP(httpRequest),
                        httpRequest.getHeader("User-Agent"),
                        false
                );
                throw new InvalidCredentialsException("Invalid credentials");
            }
        }

        // Check if account is locked
        if (user.isLocked()) {
            auditService.logAuthEvent(
                    "LOGIN_FAILED_LOCKED",
                    user.getId(),
                    user.getUsername(),
                    getClientIP(httpRequest),
                    httpRequest.getHeader("User-Agent"),
                    false
            );
            throw new AccountLockedException("Account is locked due to too many failed login attempts");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            userService.incrementFailedAttempts(
                    user.getId(),
                    appConfig.getAuth().getAccountLockout().getMaxAttempts(),
                    appConfig.getAuth().getAccountLockout().getLockoutDurationMinutes()
            );

            auditService.logAuthEvent(
                    "LOGIN_FAILED",
                    user.getId(),
                    user.getUsername(),
                    getClientIP(httpRequest),
                    httpRequest.getHeader("User-Agent"),
                    false
            );

            throw new InvalidCredentialsException("Invalid credentials");
        }

        // Update last login and reset failed attempts
        userService.updateLastLogin(user.getId());

        String accessToken = tokenService.createAccessToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(
                user.getId(),
                getDeviceInfo(httpRequest),
                getClientIP(httpRequest),
                httpRequest.getHeader("User-Agent")
        );

        auditService.logAuthEvent(
                "LOGIN_SUCCESS",
                user.getId(),
                user.getUsername(),
                getClientIP(httpRequest),
                httpRequest.getHeader("User-Agent"),
                true
        );
        return new AuthResponse(
                accessToken, "Bearer", tokenService.getAccessTokenExpirationSeconds(),
                userService.toUserInfo(user), refreshToken
        );
    }

    @Transactional
    public RefreshResponse refresh(String refreshToken, HttpServletRequest httpRequest) {
        var rotatedToken = refreshTokenService.rotateToken(
                refreshToken,
                getDeviceInfo(httpRequest),
                getClientIP(httpRequest),
                httpRequest.getHeader("User-Agent")
        );

        User user = userService.findById(rotatedToken.getUserId());
        String accessToken = tokenService.createAccessToken(user);

        auditService.logAuthEvent(
                "TOKEN_REFRESH",
                user.getId(),
                user.getUsername(),
                getClientIP(httpRequest),
                httpRequest.getHeader("User-Agent"),
                true
        );
        return new RefreshResponse(accessToken, "Bearer", tokenService.getAccessTokenExpirationSeconds(), rotatedToken.getToken());
    }

    @Transactional
    public void logout(String refreshToken, UUID userId, HttpServletRequest httpRequest) {
        if (refreshToken != null) {
            refreshTokenService.revokeToken(refreshToken);
        }

        if (userId != null) {
            User user = userService.findById(userId);
            auditService.logAuthEvent(
                    "LOGOUT",
                    userId,
                    user.getUsername(),
                    getClientIP(httpRequest),
                    httpRequest.getHeader("User-Agent"),
                    true
            );
        }
    }

    public ResponseCookie createRefreshTokenCookie(String token) {
        return ResponseCookie.from("refresh_token", token)
                .httpOnly(true)
                .secure(true)
                .path("/api/auth/refresh")
                .maxAge(Duration.ofDays(14))
                .sameSite("Strict")
                .build();
    }

    public ResponseCookie clearRefreshTokenCookie() {
        return ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(true)
                .path("/api/auth/refresh")
                .maxAge(0)
                .sameSite("Strict")
                .build();
    }

    public String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }

    public String getDeviceInfo(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            return "Unknown";
        }

        if (userAgent.contains("Mobile")) {
            return "Mobile";
        } else if (userAgent.contains("Tablet")) {
            return "Tablet";
        } else {
            return "Desktop";
        }
    }
}

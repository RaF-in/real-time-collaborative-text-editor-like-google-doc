package com.mmtext.authserver.controller;

import com.mmtext.authserver.dto.ApiResponse;
import com.mmtext.authserver.dto.UserInfo;
import com.mmtext.authserver.model.RefreshToken;
import com.mmtext.authserver.model.User;
import com.mmtext.authserver.service.RefreshTokenService;
import com.mmtext.authserver.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final RefreshTokenService refreshTokenService;

    public AdminController(UserService userService, RefreshTokenService refreshTokenService) {
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserInfo>>> getAllUsers() {
        // In production, add pagination
        return ResponseEntity.ok(ApiResponse.success(
                userService.userRepository.findAll().stream()
                        .map(userService::toUserInfo)
                        .toList()
        ));
    }

    @GetMapping("/users/{userId}/sessions")
    public ResponseEntity<ApiResponse<List<RefreshToken>>> getUserSessions(@PathVariable UUID userId) {
        List<RefreshToken> sessions = refreshTokenService.findActiveTokensForUser(userId);
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    @PostMapping("/users/{userId}/revoke-all-sessions")
    public ResponseEntity<ApiResponse<Void>> revokeAllUserSessions(@PathVariable UUID userId) {
        refreshTokenService.revokeAllForUser(userId);
        return ResponseEntity.ok(
                ApiResponse.success("All sessions revoked for user", null)
        );
    }

    @PostMapping("/users/{userId}/enable")
    public ResponseEntity<ApiResponse<Void>> enableUser(@PathVariable UUID userId) {
        User user = userService.findById(userId);
        user.setEnabled(true);
        user.resetFailedAttempts();
        userService.userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.success("User enabled successfully", null));
    }

    @PostMapping("/users/{userId}/disable")
    public ResponseEntity<ApiResponse<Void>> disableUser(@PathVariable UUID userId) {
        User user = userService.findById(userId);
        user.setEnabled(false);
        userService.userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.success("User disabled successfully", null));
    }
}
package com.mmtext.authserver.controller;

import com.mmtext.authserver.dto.ApiResponse;
import com.mmtext.authserver.dto.ChangePasswordRequest;
import com.mmtext.authserver.dto.UpdateUserRequest;
import com.mmtext.authserver.dto.UserInfo;
import com.mmtext.authserver.model.User;
import com.mmtext.authserver.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")

public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{userId}")
    @PreAuthorize("@userSecurity.canAccessUser(#userId, authentication)")
    public ResponseEntity<ApiResponse<UserInfo>> getUserById(@PathVariable UUID userId) {
        User user = userService.findById(userId);
        return ResponseEntity.ok(ApiResponse.success(userService.toUserInfo(user)));
    }

    @PutMapping("/{userId}")
    @PreAuthorize("@userSecurity.canAccessUser(#userId, authentication)")
    public ResponseEntity<ApiResponse<UserInfo>> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRequest request) {

        User user = userService.updateUser(userId, request);
        return ResponseEntity.ok(
                ApiResponse.success("User updated successfully", userService.toUserInfo(user))
        );
    }

    @PostMapping("/{userId}/change-password")
    @PreAuthorize("@userSecurity.canAccessUser(#userId, authentication)")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @PathVariable UUID userId,
            @Valid @RequestBody ChangePasswordRequest request) {

        userService.changePassword(userId, request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("@userSecurity.canAccessUser(#userId, authentication)")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable UUID userId) {
        userService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
    }
}
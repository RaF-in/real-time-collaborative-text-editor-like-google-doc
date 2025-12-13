package com.mmtext.authserver.controller;

import com.mmtext.authserver.dto.ApiResponse;
import com.mmtext.authserver.dto.UserInfo;
import com.mmtext.authserver.model.User;
import com.mmtext.authserver.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class MeController {

    private final UserService userService;

    public MeController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserInfo>> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {

        // Extract username from UserDetails
        String username = userDetails.getUsername();

        // Find user by username
        User user = userService.findByUsername(username);

        // Convert to UserInfo (excluding sensitive data)
        UserInfo userInfo = userService.toUserInfo(user);

        return ResponseEntity.ok(ApiResponse.success(userInfo));
    }
}
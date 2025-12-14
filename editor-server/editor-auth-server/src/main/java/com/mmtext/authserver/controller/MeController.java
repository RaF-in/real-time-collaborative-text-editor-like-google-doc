package com.mmtext.authserver.controller;

import com.mmtext.authserver.dto.ApiResponse;
import com.mmtext.authserver.dto.UserInfo;
import com.mmtext.authserver.model.User;
import com.mmtext.authserver.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
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
            @AuthenticationPrincipal Object principal) {

        String username;

        // Handle JWT authentication (OAuth2)
        if (principal instanceof Jwt) {
            Jwt jwt = (Jwt) principal;
            username = jwt.getClaim("username"); // Get username from JWT claim
        }
        // Handle regular UserDetails authentication (form login)
        else if (principal instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) principal;
            username = userDetails.getUsername();
        }
        // Handle authentication by email (fallback)
        else if (principal instanceof String) {
            username = (String) principal;
        } else {
            throw new IllegalStateException("Unsupported authentication principal type: " +
                (principal != null ? principal.getClass().getSimpleName() : "null"));
        }

        // Find user by username
        User user = userService.findByUsername(username);

        // Convert to UserInfo (excluding sensitive data)
        UserInfo userInfo = userService.toUserInfo(user);

        return ResponseEntity.ok(ApiResponse.success(userInfo));
    }
}
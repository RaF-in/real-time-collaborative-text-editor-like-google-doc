package com.mmtext.authserver.controller;

import com.mmtext.authserver.dto.ApiResponse;
import com.mmtext.authserver.dto.AuthResponse;
import com.mmtext.authserver.dto.LoginRequest;
import com.mmtext.authserver.dto.SignupRequest;
import com.mmtext.authserver.dto.RefreshResponse;
import com.mmtext.authserver.service.AuthService;
import com.mmtext.authserver.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
//@CrossOrigin(origins = {"http://localhost:4200", "http://client-app"}, allowCredentials = "true")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthResponse>> signup(
            @Valid @RequestBody SignupRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        AuthResponse authResponse = authService.signup(request, httpRequest);

        // Set refresh token in HTTP-only cookie
        ResponseCookie refreshCookie = authService.createRefreshTokenCookie(authResponse.getRefreshToken());
        httpResponse.addHeader("Set-Cookie", refreshCookie.toString());

        return ResponseEntity.ok(ApiResponse.success("Signup successful", authResponse));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        AuthResponse authResponse = authService.login(request, httpRequest);

        // Set refresh token in HTTP-only cookie
        ResponseCookie refreshCookie = authService.createRefreshTokenCookie(authResponse.getRefreshToken());
        httpResponse.addHeader("Set-Cookie", refreshCookie.toString());

        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshResponse>> refresh(
            HttpServletRequest request,
            HttpServletResponse httpResponse) {

        // Get refresh token from cookie
        String refreshToken = null;
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if ("refresh_token".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }

        if (refreshToken == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Refresh token not found"));
        }

        RefreshResponse refreshResponse = authService.refresh(refreshToken, request);

        // Set new refresh token in HTTP-only cookie
        ResponseCookie refreshCookie = authService.createRefreshTokenCookie(refreshResponse.getNewRefreshToken());
        httpResponse.addHeader("Set-Cookie", refreshCookie.toString());

        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", refreshResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse httpResponse) {

        // Get refresh token from cookie
        String refreshToken = null;
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if ("refresh_token".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }

        // Get user ID from authentication context (would need to be implemented)
        // For now, we'll just clear the cookie
        authService.logout(refreshToken, null, request);

        // Clear refresh token cookie
        ResponseCookie clearCookie = authService.clearRefreshTokenCookie();
        httpResponse.addHeader("Set-Cookie", clearCookie.toString());

        return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }
}
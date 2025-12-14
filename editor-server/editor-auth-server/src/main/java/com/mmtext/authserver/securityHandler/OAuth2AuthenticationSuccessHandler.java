package com.mmtext.authserver.securityHandler;

import com.mmtext.authserver.config.AppConfig;
import com.mmtext.authserver.enums.AuthProvider;
import com.mmtext.authserver.model.User;
import com.mmtext.authserver.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.Duration;


@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);
    private final UserService userService;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;
    private final AppConfig appConfig;
    private final AuthService authService;

    public OAuth2AuthenticationSuccessHandler(UserService userService, TokenService tokenService, RefreshTokenService refreshTokenService, AppConfig appConfig, AuthService authService) {
        this.userService = userService;
        this.tokenService = tokenService;
        this.refreshTokenService = refreshTokenService;
        this.appConfig = appConfig;
        this.authService = authService;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();

        try {
            // Extract user info from OAuth2 provider
            String email = oauth2User.getAttribute("email");
            String name = oauth2User.getAttribute("name");
            String providerId = oauth2User.getAttribute("sub");

            if (email == null) {
                log.error("Email not provided by OAuth2 provider");
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email not provided");
                return;
            }

            // Find or create user
            User user = userService.findOrCreateOAuth2User(
                    email,
                    name != null ? name : email.split("@")[0],
                    AuthProvider.GOOGLE,
                    providerId
            );

            // Generate tokens
            String accessToken = tokenService.createAccessToken(user);
            String refreshToken = refreshTokenService.createRefreshToken(
                    user.getId(),
                    authService.getDeviceInfo(request),
                    authService.getClientIP(request),
                    request.getHeader("User-Agent")
            );

            // Set refresh token as HTTP-only cookie
            ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
                    .httpOnly(true)
                    .secure(true)
                    .path("/api/auth/refresh")
                    .maxAge(Duration.ofDays(14))
                    .sameSite("Lax")
                    .build();
            response.addHeader("Set-Cookie", refreshCookie.toString());

            // Set CSRF token cookie (readable by JavaScript)
            String csrfToken = java.util.UUID.randomUUID().toString();
            ResponseCookie csrfCookie = ResponseCookie.from("csrf_token", csrfToken)
                    .httpOnly(false) // IMPORTANT: JS must be able to read this
                    .secure(true)
                    .path("/")
                    .maxAge(Duration.ofDays(14))
                    .sameSite("Lax")
                    .build();
            response.addHeader("Set-Cookie", csrfCookie.toString());

            // Get OAuth2 state parameter (contains return URL)
            String state = request.getParameter("state");

            // Build redirect URL with token and state
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromUriString(appConfig.getFrontend().getUrl() + appConfig.getFrontend().getOauthRedirectPath())
                    .queryParam("token", accessToken);

            // Include state parameter if it exists
            if (state != null && !state.isEmpty()) {
                builder.queryParam("state", state);
            }

            String targetUrl = builder.build().toUriString();

            log.info("OAuth2 authentication successful for user: {}", user.getEmail());
            getRedirectStrategy().sendRedirect(request, response, targetUrl);

        } catch (Exception e) {
            log.error("Error during OAuth2 authentication", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Authentication failed");
        }
    }
}
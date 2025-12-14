package com.mmtext.authserver.config;

import com.mmtext.authserver.securityHandler.JwtAuthenticationConverter;
import com.mmtext.authserver.securityHandler.OAuth2AuthenticationSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfig {

    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;
    private final JwtDecoder jwtDecoder;
    private final CorsProperties corsProperties;

    public SecurityConfig(@Lazy OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler,
                          @Lazy JwtAuthenticationConverter jwtAuthenticationConverter,
                          @Lazy JwtDecoder jwtDecoder,
                          CorsProperties corsProperties) {
        this.oAuth2AuthenticationSuccessHandler = oAuth2AuthenticationSuccessHandler;
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
        this.jwtDecoder = jwtDecoder;
        this.corsProperties = corsProperties;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/auth/**") // We handle CSRF manually with double-submit pattern
                )
                .cors(Customizer.withDefaults()) // Uses the corsConfigurationSource bean
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(authorize -> authorize
                        // Allow OPTIONS requests for CORS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Public endpoints
                        .requestMatchers(
                                "/api/auth/signup",
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/api/auth/logout",
                                "/oauth2/**",
                                "/login/oauth2/**",
                                "/.well-known/**",
                                "/actuator/health",
                                "/actuator/info"
                        ).permitAll()

                        // Admin endpoints
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )
                // OAuth2 Login Configuration
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter)
                        )
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // CRITICAL: Parse origins correctly - no wildcards allowed with credentials
        String originsStr = corsProperties.getAllowedOrigins();
        if (originsStr != null && !originsStr.trim().isEmpty()) {
            List<String> origins = Arrays.stream(originsStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty() && !s.equals("*")) // Filter out wildcards
                    .collect(Collectors.toList());

            if (origins.isEmpty()) {
                throw new IllegalArgumentException(
                        "CORS configuration error: No valid origins specified. " +
                                "Wildcards (*) are not allowed when credentials are enabled."
                );
            }

            configuration.setAllowedOrigins(origins);
        } else {
            throw new IllegalArgumentException("CORS allowed origins must be configured");
        }

        // Parse methods
        String methodsStr = corsProperties.getAllowedMethods();
        if (methodsStr != null && !methodsStr.trim().isEmpty()) {
            List<String> methods = Arrays.stream(methodsStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            configuration.setAllowedMethods(methods);
        }

        // Parse headers - no wildcards when credentials are enabled
        String headersStr = corsProperties.getAllowedHeaders();
        if (headersStr != null && !headersStr.trim().isEmpty()) {
            List<String> headers = Arrays.stream(headersStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty() && !s.equals("*"))
                    .collect(Collectors.toList());

            // If no valid headers after filtering wildcards, use common safe headers
            if (headers.isEmpty()) {
                headers = Arrays.asList(
                        "Content-Type",
                        "Authorization",
                        "X-CSRF-TOKEN",
                        "X-Requested-With"
                );
            }

            headers.forEach(configuration::addAllowedHeader);
        }

        // CRITICAL: Must be true for cookies
        configuration.setAllowCredentials(corsProperties.isAllowCredentials());

        // Expose headers so frontend can read them
        String exposedHeadersStr = corsProperties.getExposedHeaders();
        if (exposedHeadersStr != null && !exposedHeadersStr.trim().isEmpty()) {
            List<String> exposedHeaders = Arrays.stream(exposedHeadersStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            configuration.setExposedHeaders(exposedHeaders);
        } else {
            configuration.setExposedHeaders(Arrays.asList("Set-Cookie", "Authorization"));
        }

        configuration.setMaxAge(corsProperties.getMaxAge() > 0 ?
                corsProperties.getMaxAge() : 3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Argon2 is more secure than BCrypt
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
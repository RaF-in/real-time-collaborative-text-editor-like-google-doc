package com.mmtext.editorservershare.config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;


@Configuration
public class JwksConfig {

    private static final Logger log = LoggerFactory.getLogger(JwksConfig.class);

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri("http://auth-server:8080/.well-known/jwks.json").build();

        // Disable claim set validation for testing
        return jwtDecoder;
    }
}

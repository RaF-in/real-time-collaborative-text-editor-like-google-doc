package com.mmtext.authserver.config;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.UUID;

@Configuration
public class JwksConfig {

    private static final Logger log = LoggerFactory.getLogger(JwksConfig.class);
    private final JwtProperties jwtProperties;

    public JwksConfig(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @Bean
    public RSAKey rsaKey() throws Exception {
        Path keyPath = Paths.get(jwtProperties.getKey().getStoragePath());
        Path keyFile = keyPath.resolve("current-key.json");

        // Create directory if it doesn't exist
        if (!Files.exists(keyPath)) {
            Files.createDirectories(keyPath);
            log.info("Created JWT key storage directory: {}", keyPath);
        }

        // Try to load existing key
        if (Files.exists(keyFile)) {
            try {
                String keyJson = Files.readString(keyFile);
                RSAKey key = RSAKey.parse(keyJson);
                log.info("Loaded existing RSA key with kid: {}", key.getKeyID());
                return key;
            } catch (IOException | ParseException e) {
                log.warn("Failed to load existing key, generating new one", e);
            }
        }

        // Generate new key
        RSAKey newKey = generateRSAKey();

        // Save key to file
        try {
            Files.writeString(keyFile, newKey.toJSONString());
            log.info("Generated and saved new RSA key with kid: {}", newKey.getKeyID());
        } catch (IOException e) {
            log.error("Failed to save RSA key to file", e);
        }

        return newKey;
    }

    private RSAKey generateRSAKey() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
    }

    @Bean
    public JWKSet jwkSet(RSAKey rsaKey) {
        return new JWKSet(rsaKey);
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource(JWKSet jwkSet) {
        return new ImmutableJWKSet<>(jwkSet);
    }

    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public JwtDecoder jwtDecoder(RSAKey rsaKey) throws JOSEException {
        return NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build();
    }
}

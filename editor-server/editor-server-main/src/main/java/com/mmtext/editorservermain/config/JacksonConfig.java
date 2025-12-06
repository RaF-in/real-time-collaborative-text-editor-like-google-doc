package com.mmtext.editorservermain.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Jackson configuration for proper serialization/deserialization of Java 8 time types
 *
 * This configuration addresses the issue where java.time.Instant and other JSR310 types
 * cannot be serialized by default Jackson ObjectMapper.
 */
@Configuration
public class JacksonConfig {

    /**
     * Configure primary ObjectMapper with JSR310 module support
     * This bean will be used by Spring Boot for all JSON serialization/deserialization
     *
     * @return Configured ObjectMapper instance
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        // Create a new ObjectMapper instance
        ObjectMapper mapper = new ObjectMapper();

        // Register JavaTimeModule to handle JSR310 types (Instant, LocalDate, etc.)
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        mapper.registerModule(javaTimeModule);

        // Disable writing dates as timestamps (e.g., 1625097600000)
        // Instead, dates will be serialized as ISO-8601 strings (e.g., "2021-07-01T00:00:00Z")
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Additional useful configurations
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // Log successful configuration
        System.out.println("✅ JacksonConfig: JavaTimeModule registered successfully");

        return mapper;
    }
}
package com.mmtext.editorservermain.config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "cors")
public class CorsProperties {
    private String allowedOrigins = "http://localhost:4200";
    private String allowedMethods = "GET,POST,PUT,DELETE,OPTIONS";
    private String allowedHeaders = "Content-Type,Authorization,X-CSRF-TOKEN,DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Range";
    private boolean allowCredentials = true;
    private long maxAge = 3600;

    public String getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(String allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public String getAllowedMethods() {
        return allowedMethods;
    }

    public void setAllowedMethods(String allowedMethods) {
        this.allowedMethods = allowedMethods;
    }

    public String getAllowedHeaders() {
        return allowedHeaders;
    }

    public void setAllowedHeaders(String allowedHeaders) {
        this.allowedHeaders = allowedHeaders;
    }

    public boolean isAllowCredentials() {
        return allowCredentials;
    }

    public void setAllowCredentials(boolean allowCredentials) {
        this.allowCredentials = allowCredentials;
    }

    public long getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(long maxAge) {
        this.maxAge = maxAge;
    }
}
package com.mmtext.authserver.dto;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
public class UserInfo {
    private UUID id;
    private String username;
    private String email;
    private String provider;
    private Boolean enabled;
    private Boolean emailVerified;
    private Set<String> roles;
    private Set<String> permissions;
    private Instant createdAt;
    private Instant lastLoginAt;
    public UserInfo(){}
    public UserInfo(UUID id, String username, String email, String provider, Boolean enabled, Boolean emailVerified, Set<String> roles, Set<String> permissions, Instant createdAt, Instant lastLoginAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.provider = provider;
        this.enabled = enabled;
        this.emailVerified = emailVerified;
        this.roles = roles;
        this.permissions = permissions;
        this.createdAt = createdAt;
        this.lastLoginAt = lastLoginAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
}

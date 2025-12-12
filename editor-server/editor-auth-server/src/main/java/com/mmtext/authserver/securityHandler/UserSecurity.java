package com.mmtext.authserver.securityHandler;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("userSecurity")
public class UserSecurity {

    public boolean canAccessUser(UUID userId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        // Admins can access any user
        if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            return true;
        }

        // Users can only access their own data
        try {
            UUID authenticatedUserId = UUID.fromString(authentication.getName());
            return authenticatedUserId.equals(userId);
        } catch (Exception e) {
            return false;
        }
    }
}

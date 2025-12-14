package com.mmtext.editorservermain.securityHandler;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter defaultGrantedAuthoritiesConverter =
            new JwtGrantedAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);

        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        // Extract roles from JWT claims
        List<String> roles = jwt.getClaimAsStringList("roles");
        List<String> permissions = jwt.getClaimAsStringList("permissions");

        Collection<GrantedAuthority> authorities = new java.util.HashSet<>();

        // Add roles with ROLE_ prefix if not present
        if (roles != null && !roles.isEmpty()) {
            roles.stream()
                    .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                    .map(SimpleGrantedAuthority::new)
                    .forEach(authorities::add);
        }

        // Add permissions and add PERMISSION_ prefix if not present
        if (permissions != null && !permissions.isEmpty()) {
            permissions.stream()
                    .filter(perm -> null != perm && !perm.trim().isEmpty())
                    .map(this::normalizePermission) // Normalize permission format
                    .map(SimpleGrantedAuthority::new)
                    .forEach(authorities::add);
        }

        return authorities;
    }

    /**
     * Normalize permission format from colon to underscore and add PERMISSION_ prefix
     * e.g., "user:read" -> "PERMISSION_USER_READ"
     * e.g., "DOCUMENT_READ" -> "PERMISSION_DOCUMENT_READ"
     */
    private String normalizePermission(String permission) {
        String normalized = permission.toUpperCase().replace(':', '_');
        return normalized.startsWith("PERMISSION_") ? normalized : "PERMISSION_" + normalized;
    }
}

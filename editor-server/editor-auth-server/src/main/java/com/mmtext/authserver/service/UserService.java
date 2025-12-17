package com.mmtext.authserver.service;

import com.mmtext.authserver.dto.UpdateUserRequest;
import com.mmtext.authserver.dto.UserInfo;
import com.mmtext.authserver.dto.UserPrincipal;
import com.mmtext.authserver.enums.AuthProvider;
import com.mmtext.authserver.exception.ResourceNotFoundException;
import com.mmtext.authserver.exception.UserAlreadyExistsException;
import com.mmtext.authserver.model.Permission;
import com.mmtext.authserver.model.Role;
import com.mmtext.authserver.model.User;
import com.mmtext.authserver.repo.PermissionRepository;
import com.mmtext.authserver.repo.RoleRepository;
import com.mmtext.authserver.repo.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
public class UserService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    public final UserRepository userRepository; // Made public for admin controller
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository,
                      PermissionRepository permissionRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return new UserPrincipal(user);
    }

    @Transactional
    public User createUser(String username, String email, String password) {
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("Email already exists: " + email);
        }

        if (userRepository.existsByUsername(username)) {
            throw new UserAlreadyExistsException("Username already exists: " + username);
        }

        // Get default USER role
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new ResourceNotFoundException("Default role not found"));
        User user = new User(
                username, email, passwordEncoder.encode(password), AuthProvider.LOCAL, null, true,
                false, 0, Set.of(userRole));

        User savedUser = userRepository.save(user);

        // Ensure user has all necessary permissions
        ensureUserPermissions(savedUser.getId());

        log.info("Created new user: {}", savedUser.getUsername());

        return savedUser;
    }

    @Transactional
    public User findOrCreateOAuth2User(String email, String username, AuthProvider provider, String providerId) {
        // Try to find existing user by email
        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            User user = existingUser.get();

            // Update provider info if different
            if (user.getProvider() == AuthProvider.LOCAL) {
                user.setProvider(provider);
                user.setProviderId(providerId);
                user.setEmailVerified(true);
                user = userRepository.save(user);
                log.info("Linked OAuth2 provider {} to existing user: {}", provider, user.getUsername());
            }

            return user;
        }

        // Try to find by provider and provider ID
        Optional<User> providerUser = userRepository.findByProviderAndProviderId(provider, providerId);
        if (providerUser.isPresent()) {
            return providerUser.get();
        }

        // Create new user
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new ResourceNotFoundException("Default role not found"));

        // Ensure unique username
        String finalUsername = username;
        int counter = 1;
        while (userRepository.existsByUsername(finalUsername)) {
            finalUsername = username + counter++;
        }
        User newUser = new User(
                finalUsername, email, null, provider, providerId, true,
                true, 0, Set.of(userRole));

        User savedUser = userRepository.save(newUser);
        log.info("Created new OAuth2 user: {} with provider: {}", savedUser.getUsername(), provider);

        return savedUser;
    }

    public User findById(UUID userId) {
        return userRepository.findById(userId)
                .filter(user -> !user.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    /**
     * Find user by ID - returns Optional for gRPC compatibility
     */
    public Optional<User> getUserById(UUID userId) {
        return userRepository.findById(userId)
                .filter(user -> user.getEnabled() != null && user.getEnabled() && !user.isDeleted());
    }

    /**
     * Find user by email - returns Optional for gRPC compatibility
     */
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .filter(user -> user.getEnabled() != null && user.getEnabled() && !user.isDeleted());
    }

    /**
     * Get all active users for gRPC search functionality
     */
    public List<User> getAllUsers() {
        return userRepository.findAll().stream()
                .filter(user -> user.getEnabled() != null && user.getEnabled() && !user.isDeleted())
                .collect(Collectors.toList());
    }

    @Transactional
    public User updateUser(UUID userId, UpdateUserRequest request) {
        User user = findById(userId);

        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new UserAlreadyExistsException("Username already exists");
            }
            user.setUsername(request.getUsername());
        }

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new UserAlreadyExistsException("Email already exists");
            }
            user.setEmail(request.getEmail());
            user.setEmailVerified(false);
        }

        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        User user = findById(userId);

        if (user.getProvider() != AuthProvider.LOCAL) {
            throw new IllegalStateException("Cannot change password for OAuth2 users");
        }

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Password changed for user: {}", user.getUsername());
    }

    @Transactional
    public void deleteUser(UUID userId) {
        User user = findById(userId);
        user.setDeletedAt(Instant.now());
        user.setEnabled(false);
        userRepository.save(user);

        log.info("Soft deleted user: {}", user.getUsername());
    }

    @Transactional
    public void updateLastLogin(UUID userId) {
        User user = findById(userId);
        user.setLastLoginAt(Instant.now());
        user.resetFailedAttempts();
        userRepository.save(user);
    }

    @Transactional
    public void incrementFailedAttempts(UUID userId, int maxAttempts, int lockoutDurationMinutes) {
        User user = findById(userId);
        user.incrementFailedAttempts();

        if (user.getFailedLoginAttempts() >= maxAttempts) {
            user.lock(lockoutDurationMinutes);
            log.warn("User account locked due to failed login attempts: {}", user.getUsername());
        }

        userRepository.save(user);
    }

    public UserInfo toUserInfo(User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        Set<String> permissionNames = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getName)
                .collect(Collectors.toSet());
        return new UserInfo(
                user.getId(), user.getUsername(), user.getEmail(), user.getProvider().name(),
                user.getEnabled(), user.getEmailVerified(), roleNames, permissionNames,
                user.getCreatedAt(), user.getLastLoginAt()
        );
    }

    /**
     * Ensure ROLE_USER has all necessary document permissions
     * This method can be called after user creation to verify permissions
     */
    @Transactional
    public void ensureUserPermissions(UUID userId) {
        User user = findById(userId);
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new ResourceNotFoundException("ROLE_USER not found"));

        // Check if user has ROLE_USER
        if (!user.getRoles().contains(userRole)) {
            user.getRoles().add(userRole);
            userRepository.save(user);
            log.info("Added ROLE_USER to user: {}", user.getUsername());
        }

        // Required document permissions for regular users
        String[] requiredPermissions = {
            "DOCUMENT_CREATE",
            "DOCUMENT_READ",
            "DOCUMENT_WRITE",
            "DOCUMENT_DELETE",
            "DOCUMENT_LIST",
            "DOCUMENT_ACCESS"
        };

        // Check and add missing permissions to ROLE_USER
        for (String permissionName : requiredPermissions) {
            if (!userRole.hasPermission(permissionName)) {
                // Find the permission
                permissionRepository.findByName(permissionName)
                    .ifPresent(permission -> {
                        userRole.getPermissions().add(permission);
                        roleRepository.save(userRole);
                        log.info("Added permission {} to ROLE_USER", permissionName);
                    });
            }
        }
    }

    /**
     * Debug method to check user permissions
     */
    public Set<String> getUserPermissions(UUID userId) {
        User user = findById(userId);
        return user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getName)
                .collect(Collectors.toSet());
    }
}


package com.mmtext.editorservershare.client.grpc;

import com.google.protobuf.Timestamp;
import com.mmtext.auth.grpc.AuthServiceGrpc;
import com.mmtext.auth.grpc.AuthServiceProto;
import com.mmtext.common.grpc.CommonProto;
import com.mmtext.editorservershare.domain.User;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.ManagedChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * gRPC client for Auth Service
 */
@Component
public class AuthServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceClient.class);

    private final ManagedChannel authServiceChannel;

    @Autowired
    public AuthServiceClient(ManagedChannel authServiceChannel) {
        this.authServiceChannel = authServiceChannel;
    }

    /**
     * Get user by ID
     */
    @CircuitBreaker(name = "authService")
    @Retry(name = "authService")
    public Optional<User> getUserById(String userId) {
        try {
            AuthServiceGrpc.AuthServiceBlockingStub stub = AuthServiceGrpc.newBlockingStub(authServiceChannel);

            AuthServiceProto.GetUserByIdRequest request = AuthServiceProto.GetUserByIdRequest.newBuilder()
                    .setUserId(userId)
                    .build();

            AuthServiceProto.GetUserByIdResponse response = stub.getUserById(request);

            if (response.hasUser()) {
                return Optional.of(convertGrpcUserToDomain(response.getUser()));
            }

            if (response.hasError()) {
                log.warn("Error getting user by ID {}: {}", userId, response.getError());
            }

            return Optional.empty();
        } catch (StatusRuntimeException e) {
            log.error("Failed to get user by ID: {}", userId, e);
            return Optional.empty();
        }
    }

    /**
     * Get user by email
     */
    @CircuitBreaker(name = "authService")
    @Retry(name = "authService")
    public Optional<User> getUserByEmail(String email) {
        try {
            AuthServiceGrpc.AuthServiceBlockingStub stub = AuthServiceGrpc.newBlockingStub(authServiceChannel);

            AuthServiceProto.GetUserByEmailRequest request = AuthServiceProto.GetUserByEmailRequest.newBuilder()
                    .setEmail(email)
                    .build();

            AuthServiceProto.GetUserByEmailResponse response = stub.getUserByEmail(request);

            if (response.hasUser()) {
                return Optional.of(convertGrpcUserToDomain(response.getUser()));
            }

            if (response.hasError()) {
                log.warn("Error getting user by email {}: {}", email, response.getError());
            }

            return Optional.empty();
        } catch (StatusRuntimeException e) {
            log.error("Failed to get user by email: {}", email, e);
            return Optional.empty();
        }
    }

    /**
     * Get multiple users by IDs
     */
    @CircuitBreaker(name = "authService")
    @Retry(name = "authService")
    public Map<String, User> getUsersByIds(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            AuthServiceGrpc.AuthServiceBlockingStub stub = AuthServiceGrpc.newBlockingStub(authServiceChannel);

            AuthServiceProto.GetUsersByIdsRequest request = AuthServiceProto.GetUsersByIdsRequest.newBuilder()
                    .addAllUserIds(userIds)
                    .build();

            AuthServiceProto.GetUsersByIdsResponse response = stub.getUsersByIds(request);

            Map<String, User> result = new HashMap<>();
            response.getUsersList().forEach(grpcUser -> {
                User user = convertGrpcUserToDomain(grpcUser);
                result.put(user.getId().toString(), user);
            });

            // Log any errors for specific user IDs
            if (!response.getErrorsMap().isEmpty()) {
                response.getErrorsMap().forEach((userId, error) -> {
                    log.warn("Error getting user {}: {}", userId, error);
                });
            }

            return result;
        } catch (StatusRuntimeException e) {
            log.error("Failed to get users by IDs", e);
            return Collections.emptyMap();
        }
    }

    /**
     * Search users by email pattern
     */
    @CircuitBreaker(name = "authService")
    @Retry(name = "authService")
    public List<User> searchUsers(String emailPattern, int limit, int offset) {
        try {
            AuthServiceGrpc.AuthServiceBlockingStub stub = AuthServiceGrpc.newBlockingStub(authServiceChannel);

            AuthServiceProto.SearchUsersRequest request = AuthServiceProto.SearchUsersRequest.newBuilder()
                    .setEmailPattern(emailPattern)
                    .setLimit(limit)
                    .setOffset(offset)
                    .build();

            AuthServiceProto.SearchUsersResponse response = stub.searchUsers(request);

            return response.getUsersList().stream()
                    .map(this::convertGrpcUserToDomain)
                    .collect(Collectors.toList());
        } catch (StatusRuntimeException e) {
            log.error("Failed to search users with pattern: {}", emailPattern, e);
            return Collections.emptyList();
        }
    }

    /**
     * Validate user exists
     */
    @CircuitBreaker(name = "authService")
    @Retry(name = "authService")
    public boolean validateUser(String userId) {
        try {
            AuthServiceGrpc.AuthServiceBlockingStub stub = AuthServiceGrpc.newBlockingStub(authServiceChannel);

            AuthServiceProto.ValidateUserRequest request = AuthServiceProto.ValidateUserRequest.newBuilder()
                    .setUserId(userId)
                    .build();

            AuthServiceProto.ValidateUserResponse response = stub.validateUser(request);
            return response.getValid();
        } catch (StatusRuntimeException e) {
            log.error("Failed to validate user: {}", userId, e);
            return false;
        }
    }

    /**
     * Convert gRPC User to domain User
     */
    private User convertGrpcUserToDomain(CommonProto.User grpcUser) {
        return User.builder()
                .id(UUID.fromString(grpcUser.getId()))
                .email(grpcUser.getEmail())
                .firstName(grpcUser.getFirstName())
                .lastName(grpcUser.getLastName())
                .fullName(grpcUser.getFullName())
                .avatarUrl(grpcUser.getAvatarUrl())
                .active(grpcUser.getActive())
                .createdAt(convertTimestampToInstant(grpcUser.getCreatedAt()))
                .updatedAt(convertTimestampToInstant(grpcUser.getUpdatedAt()))
                .build();
    }

    /**
     * Convert protobuf Timestamp to Instant
     */
    private Instant convertTimestampToInstant(Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }
}
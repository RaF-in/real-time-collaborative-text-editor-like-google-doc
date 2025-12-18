package com.mmtext.authserver.grpc;

import com.google.protobuf.Timestamp;
import com.mmtext.auth.grpc.AuthServiceGrpc;
import com.mmtext.auth.grpc.AuthServiceProto;
import com.mmtext.common.grpc.CommonProto;
import com.mmtext.authserver.model.User;
import com.mmtext.authserver.service.UserService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * gRPC server implementation for Auth Service
 */
@Service
@GrpcService
public class AuthServiceGrpcImpl extends AuthServiceGrpc.AuthServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceGrpcImpl.class);

    private final UserService userService;

    @Autowired
    public AuthServiceGrpcImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void getUserById(AuthServiceProto.GetUserByIdRequest request, StreamObserver<AuthServiceProto.GetUserByIdResponse> responseObserver) {
        try {
            log.debug("Getting user by ID: {}", request.getUserId());

            Optional<User> userOpt = userService.getUserById(UUID.fromString(request.getUserId()));

            AuthServiceProto.GetUserByIdResponse.Builder responseBuilder = AuthServiceProto.GetUserByIdResponse.newBuilder();

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if (user.getEnabled() && !user.isDeleted()) {
                    CommonProto.User grpcUser = convertUserToGrpc(user);
                    responseBuilder.setUser(grpcUser);
                } else {
                    responseBuilder.setError("User is not active");
                }
            } else {
                responseBuilder.setError("User not found");
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error getting user by ID: {}", request.getUserId(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getUserByEmail(AuthServiceProto.GetUserByEmailRequest request, StreamObserver<AuthServiceProto.GetUserByEmailResponse> responseObserver) {
        try {
            log.debug("Getting user by email: {}", request.getEmail());

            Optional<User> userOpt = userService.getUserByEmail(request.getEmail());

            AuthServiceProto.GetUserByEmailResponse.Builder responseBuilder = AuthServiceProto.GetUserByEmailResponse.newBuilder();

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if (user.getEnabled() && !user.isDeleted()) {
                    CommonProto.User grpcUser = convertUserToGrpc(user);
                    responseBuilder.setUser(grpcUser);
                } else {
                    responseBuilder.setError("User is not active");
                }
            } else {
                responseBuilder.setError("User not found");
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error getting user by email: {}", request.getEmail(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getUsersByIds(AuthServiceProto.GetUsersByIdsRequest request, StreamObserver<AuthServiceProto.GetUsersByIdsResponse> responseObserver) {
        try {
            log.debug("Getting {} users by IDs", request.getUserIdsCount());

            AuthServiceProto.GetUsersByIdsResponse.Builder responseBuilder = AuthServiceProto.GetUsersByIdsResponse.newBuilder();
            List<String> errors = new ArrayList<>();

            for (String userId : request.getUserIdsList()) {
                try {
                    Optional<User> userOpt = userService.getUserById(UUID.fromString(userId));
                    if (userOpt.isPresent()) {
                        User user = userOpt.get();
                        if (user.getEnabled() && !user.isDeleted()) {
                            CommonProto.User grpcUser = convertUserToGrpc(user);
                            responseBuilder.addUsers(grpcUser);
                        } else {
                            errors.add(userId + ": User is not active");
                        }
                    } else {
                        errors.add(userId + ": User not found");
                    }
                } catch (Exception e) {
                    errors.add(userId + ": " + e.getMessage());
                    log.warn("Error getting user {}: {}", userId, e.getMessage());
                }
            }

            // Add all errors to the response
            for (String error : errors) {
                String[] parts = error.split(":", 2);
                if (parts.length == 2) {
                    responseBuilder.putErrors(parts[0], parts[1]);
                }
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error getting users by IDs", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void searchUsers(AuthServiceProto.SearchUsersRequest request, StreamObserver<AuthServiceProto.SearchUsersResponse> responseObserver) {
        try {
            log.debug("Searching users with pattern: {}", request.getEmailPattern());

            // This is a simple implementation - in a real system, you'd use proper email pattern matching
            List<User> allUsers = userService.getAllUsers();
            List<User> filteredUsers = allUsers.stream()
                    .filter(user -> user.getEnabled() && !user.isDeleted())
                    .filter(user -> user.getEmail().toLowerCase().contains(request.getEmailPattern().toLowerCase()))
                    .collect(Collectors.toList());

            // Apply pagination
            int offset = request.getOffset();
            int limit = request.getLimit();
            int total = filteredUsers.size();

            List<User> paginatedUsers = filteredUsers.stream()
                    .skip(offset)
                    .limit(limit)
                    .collect(Collectors.toList());

            AuthServiceProto.SearchUsersResponse.Builder responseBuilder = AuthServiceProto.SearchUsersResponse.newBuilder()
                    .setTotalCount(total);

            for (User user : paginatedUsers) {
                CommonProto.User grpcUser = convertUserToGrpc(user);
                responseBuilder.addUsers(grpcUser);
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error searching users", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void validateUser(AuthServiceProto.ValidateUserRequest request, StreamObserver<AuthServiceProto.ValidateUserResponse> responseObserver) {
        try {
            log.debug("Validating user: {}", request.getUserId());

            Optional<User> userOpt = userService.getUserById(UUID.fromString(request.getUserId()));

            AuthServiceProto.ValidateUserResponse.Builder responseBuilder = AuthServiceProto.ValidateUserResponse.newBuilder()
                    .setValid(false);

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                boolean isValid = user.getEnabled() && !user.isDeleted();
                responseBuilder.setValid(isValid);

                if (isValid) {
                    CommonProto.User grpcUser = convertUserToGrpc(user);
                    responseBuilder.setUser(grpcUser);
                }
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error validating user: {}", request.getUserId(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    /**
     * Convert domain User to gRPC User
     */
    private CommonProto.User convertUserToGrpc(User user) {
        CommonProto.User.Builder builder = CommonProto.User.newBuilder()
                .setId(user.getId().toString())
                .setEmail(user.getEmail())
                .setActive(user.getEnabled() != null && user.getEnabled() && !user.isDeleted());

        if (user.getFirstName() != null) {
            builder.setFirstName(user.getFirstName());
        }

        if (user.getLastName() != null) {
            builder.setLastName(user.getLastName());
        }

        // Combine first and last name for full name
        String fullName = "";
        if (user.getFirstName() != null && user.getLastName() != null) {
            fullName = user.getFirstName() + " " + user.getLastName();
        } else if (user.getFirstName() != null) {
            fullName = user.getFirstName();
        } else if (user.getLastName() != null) {
            fullName = user.getLastName();
        } else {
            fullName = user.getEmail();
        }

        builder.setFullName(fullName);

        if (user.getAvatarUrl() != null) {
            builder.setAvatarUrl(user.getAvatarUrl());
        }

        if (user.getCreatedAt() != null) {
            builder.setCreatedAt(convertInstantToTimestamp(user.getCreatedAt()));
        }

        if (user.getUpdatedAt() != null) {
            builder.setUpdatedAt(convertInstantToTimestamp(user.getUpdatedAt()));
        }

        return builder.build();
    }

    /**
     * Convert Instant to protobuf Timestamp
     */
    private Timestamp convertInstantToTimestamp(Instant instant) {
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }
}
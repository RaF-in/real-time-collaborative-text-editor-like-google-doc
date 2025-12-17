# Editor Server Share Module - gRPC Implementation Plan

## Overview
This document outlines the implementation plan for using gRPC for inter-service communication between editor-server-share, editor-server-snapshot, and auth-server modules.

## Why gRPC?

### Advantages
- **High Performance**: Uses HTTP/2 and Protocol Buffers for efficient serialization
- **Type Safety**: Strongly typed service definitions with proto files
- **Bidirectional Streaming**: Support for streaming data
- **Code Generation**: Auto-generated client and server stubs
- **Cross-Language**: Language-agnostic protocol
- **Efficient**: Binary serialization (smaller payloads, faster parsing)

### Architecture Overview
```
┌─────────────────────┐    gRPC     ┌──────────────────────┐
│ Editor-Server-Share │ <--------> │   Editor-Server-     │
│   (Client)          │            │   Snapshot (Server) │
└─────────────────────┘            └──────────────────────┘
         │                                   │
         │ gRPC                              │
         ↓                                   ↓
┌─────────────────────┐    gRPC     ┌──────────────────────┐
│ Auth-Server         │ <--------> │ Auth-Server (Server) │
└─────────────────────┘            └──────────────────────┘
```

## Phase 1: Proto File Definitions

### 1.1 Common Proto Types (`common.proto`)
```protobuf
syntax = "proto3";

package com.mmtext.common;

import "google/protobuf/timestamp.proto";
import "google/protobuf/empty.proto";

option java_package = "com.mmtext.common.grpc";
option java_outer_classname = "CommonProto";

// Common message types
message User {
  string id = 1;
  string email = 2;
  string first_name = 3;
  string last_name = 4;
  string full_name = 5;
  string avatar_url = 6;
  bool active = 7;
  google.protobuf.Timestamp created_at = 8;
  google.protobuf.Timestamp updated_at = 9;
}

message Document {
  string id = 1;
  string doc_id = 2;
  string title = 3;
  string owner_id = 4;
  google.protobuf.Timestamp created_at = 5;
  google.protobuf.Timestamp updated_at = 6;
  bool allow_access_requests = 7;
}

message UserList {
  repeated User users = 1;
}

message UserIds {
  repeated string user_ids = 1;
}

message UserEmails {
  repeated string emails = 1;
}

// Standard response wrapper
message Response {
  bool success = 1;
  string message = 2;
  int32 status_code = 3;
}
```

### 1.2 Auth Service Proto (`auth_service.proto`)
```protobuf
syntax = "proto3";

package com.mmtext.auth;

import "common.proto";
import "google/protobuf/empty.proto";

option java_package = "com.mmtext.auth.grpc";
option java_outer_classname = "AuthServiceProto";

service AuthService {
  // Get user by ID
  rpc GetUserById(GetUserByIdRequest) returns (GetUserByIdResponse);

  // Get user by email
  rpc GetUserByEmail(GetUserByEmailRequest) returns (GetUserByEmailResponse);

  // Get multiple users by IDs
  rpc GetUsersByIds(GetUsersByIdsRequest) returns (GetUsersByIdsResponse);

  // Search users by email pattern
  rpc SearchUsers(SearchUsersRequest) returns (SearchUsersResponse);

  // Validate user exists
  rpc ValidateUser(ValidateUserRequest) returns (ValidateUserResponse);
}

message GetUserByIdRequest {
  string user_id = 1;
}

message GetUserByIdResponse {
  oneof result {
    User user = 1;
    string error = 2;
  }
}

message GetUserByEmailRequest {
  string email = 1;
}

message GetUserByEmailResponse {
  oneof result {
    User user = 1;
    string error = 2;
  }
}

message GetUsersByIdsRequest {
  repeated string user_ids = 1;
}

message GetUsersByIdsResponse {
  repeated User users = 1;
  map<string, string> errors = 2; // user_id -> error message
}

message SearchUsersRequest {
  string email_pattern = 1;
  int32 limit = 2;
  int32 offset = 3;
}

message SearchUsersResponse {
  repeated User users = 1;
  int32 total_count = 2;
}

message ValidateUserRequest {
  string user_id = 1;
}

message ValidateUserResponse {
  bool valid = 1;
  User user = 2; // Optional, populated if valid is true
}
```

### 1.3 Editor Service Proto (`editor_service.proto`)
```protobuf
syntax = "proto3";

package com.mmtext.editor;

import "common.proto";

option java_package = "com.mmtext.editor.grpc";
option java_outer_classname = "EditorServiceProto";

service EditorService {
  // Get document information
  rpc GetDocument(GetDocumentRequest) returns (GetDocumentResponse);

  // Check if document exists
  rpc DocumentExists(DocumentExistsRequest) returns (DocumentExistsResponse);

  // Get document with access control
  rpc GetDocumentWithAccess(GetDocumentWithAccessRequest) returns (GetDocumentWithAccessResponse);

  // Update document sharing settings
  rpc UpdateSharingSettings(UpdateSharingSettingsRequest) returns (Response);

  // Batch get documents
  rpc GetDocumentsByIds(GetDocumentsByIdsRequest) returns (GetDocumentsByIdsResponse);

  // Stream document updates (for future use)
  rpc StreamDocumentUpdates(StreamDocumentUpdatesRequest) returns (stream DocumentUpdate);
}

message GetDocumentRequest {
  string document_id = 1;
  string requestor_id = 2; // User making the request
}

message GetDocumentResponse {
  oneof result {
    Document document = 1;
    string error = 2;
  }
}

message DocumentExistsRequest {
  string document_id = 1;
}

message DocumentExistsResponse {
  bool exists = 1;
  string document_id = 2;
}

message GetDocumentWithAccessRequest {
  string document_id = 1;
  string requestor_id = 2;
}

message GetDocumentWithAccessResponse {
  oneof result {
    DocumentWithAccess document = 1;
    string error = 2;
  }
}

message DocumentWithAccess {
  Document document = 1;
  PermissionLevel permission_level = 2;
  bool can_share = 3;
  bool can_edit = 4;
}

enum PermissionLevel {
  NONE = 0;
  READ = 1;
  WRITE = 2;
  OWNER = 3;
}

message UpdateSharingSettingsRequest {
  string document_id = 1;
  string owner_id = 2;
  bool allow_access_requests = 3;
  string updated_by = 4;
}

message GetDocumentsByIdsRequest {
  repeated string document_ids = 1;
  string requestor_id = 2;
}

message GetDocumentsByIdsResponse {
  repeated Document documents = 1;
  map<string, string> errors = 2; // document_id -> error message
}

message StreamDocumentUpdatesRequest {
  string document_id = 1;
  string user_id = 2;
}

message DocumentUpdate {
  string document_id = 1;
  google.protobuf.Timestamp timestamp = 2;
  string update_type = 3;
  string updated_by = 4;
}
```

## Phase 2: Implementation Steps

### 2.1 Add Dependencies to pom.xml
```xml
<dependencies>
    <!-- gRPC Spring Boot Starter -->
    <dependency>
        <groupId>net.devh</groupId>
        <artifactId>grpc-spring-boot-starter</artifactId>
        <version>3.1.0.RELEASE</version>
    </dependency>

    <!-- gRPC -->
    <dependency>
        <groupId>io.grpc</groupId>
        <artifactId>grpc-netty-shaded</artifactId>
        <version>1.58.0</version>
        <scope>runtime</scope>
    </dependency>

    <dependency>
        <groupId>io.grpc</groupId>
        <artifactId>grpc-protobuf</artifactId>
        <version>1.58.0</version>
    </dependency>

    <dependency>
        <groupId>io.grpc</groupId>
        <artifactId>grpc-stub</artifactId>
        <version>1.58.0</version>
    </dependency>

    <!-- For generating gRPC code -->
    <dependency>
        <groupId>io.grpc</groupId>
        <artifactId>grpc-services</artifactId>
        <version>1.58.0</version>
    </dependency>
</dependencies>

<build>
    <extensions>
        <extension>
            <groupId>kr.motd.maven</groupId>
            <artifactId>os-maven-plugin</artifactId>
            <version>1.7.1</version>
        </extension>
    </extensions>

    <plugins>
        <plugin>
            <groupId>org.xolstice.maven.plugins</groupId>
            <artifactId>protobuf-maven-plugin</artifactId>
            <version>0.6.1</version>
            <configuration>
                <protocArtifact>com.google.protobuf:protoc:3.24.4:exe:${os.detected.classifier}</protocArtifact>
                <pluginId>grpc-java</pluginId>
                <pluginArtifact>io.grpc:protoc-gen-grpc-java:1.58.0:exe:${os.detected.classifier}</pluginArtifact>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>compile</goal>
                        <goal>compile-custom</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### 2.2 Project Structure
```
src/
├── main/
│   ├── java/
│   │   └── com/mmtext/editorservershare/
│   │       ├── client/
│   │       │   ├── gprc/
│   │       │   │   ├── AuthServiceClient.java
│   │       │   │   └── EditorServiceClient.java
│   │       │   └── ServiceClientFactory.java
│   │       ├── config/
│   │       │   └── GrpcConfig.java
│   │       └── service/
│   │           ├── DocumentSharingService.java (updated)
│   │           └── AccessRequestService.java (updated)
│   └── proto/
│       ├── common.proto
│       ├── auth_service.proto
│       └── editor_service.proto
└── generated/
    └── source/
        └── proto/
            └── grpc-java/
```

### 2.3 gRPC Configuration
```java
@Configuration
@EnableGrpcClients
public class GrpcConfig {

    @Bean
    public ManagedChannel authServiceChannel(
            @Value("${grpc.auth.service.host:localhost}") String host,
            @Value("${grpc.auth.service.port:9090}") int port) {
        return ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .keepAliveTime(30, TimeUnit.SECONDS)
                .keepAliveTimeout(5, TimeUnit.SECONDS)
                .keepAliveWithoutCalls(true)
                .build();
    }

    @Bean
    public ManagedChannel editorServiceChannel(
            @Value("${grpc.editor.service.host:localhost}") String host,
            @Value("${grpc.editor.service.port:9091}") int port) {
        return ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .keepAliveTime(30, TimeUnit.SECONDS)
                .keepAliveTimeout(5, TimeUnit.SECONDS)
                .keepAliveWithoutCalls(true)
                .build();
    }
}
```

### 2.4 Application Configuration
```yaml
# Add to application.yml
spring:
  application:
    name: editor-server-share

grpc:
  server:
    port: ${GRPC_SERVER_PORT:9092}
  client:
    auth-service:
      address: 'static://${grpc.auth.service.host:localhost}:${grpc.auth.service.port:9090}'
      negotiation-type: plaintext
    editor-service:
      address: 'static://${grpc.editor.service.host:localhost}:${grpc.editor.service.port:9091}'
      negotiation-type: plaintext

# Custom gRPC service configuration
grpc:
  auth:
    service:
      host: ${AUTH_GRPC_HOST:localhost}
      port: ${AUTH_GRPC_PORT:9090}
  editor:
    service:
      host: ${EDITOR_GRPC_HOST:localhost}
      port: ${EDITOR_GRPC_PORT:9091}
```

## Phase 3: Client Implementation

### 3.1 Auth Service Client
```java
@GrpcClient("auth-service")
public interface AuthServiceGrpcClient extends AuthServiceGrpc.AuthServiceBlockingStub {
}

@Component
public class AuthServiceClient {

    @Autowired
    private AuthServiceGrpcClient authClient;

    @Retry(name = "authService")
    @CircuitBreaker(name = "authService")
    public Optional<User> getUserById(String userId) {
        try {
            GetUserByIdRequest request = GetUserByIdRequest.newBuilder()
                    .setUserId(userId)
                    .build();

            GetUserByIdResponse response = authClient.getUserById(request);

            if (response.hasUser()) {
                return Optional.of(convertGrpcUserToUser(response.getUser()));
            }
            return Optional.empty();
        } catch (StatusRuntimeException e) {
            log.error("Failed to get user by ID: {}", userId, e);
            return Optional.empty();
        }
    }

    public Optional<User> getUserByEmail(String email) {
        try {
            GetUserByEmailRequest request = GetUserByEmailRequest.newBuilder()
                    .setEmail(email)
                    .build();

            GetUserByEmailResponse response = authClient.getUserByEmail(request);

            if (response.hasUser()) {
                return Optional.of(convertGrpcUserToUser(response.getUser()));
            }
            return Optional.empty();
        } catch (StatusRuntimeException e) {
            log.error("Failed to get user by email: {}", email, e);
            return Optional.empty();
        }
    }

    public Map<String, User> getUsersByIds(List<String> userIds) {
        try {
            GetUsersByIdsRequest request = GetUsersByIdsRequest.newBuilder()
                    .addAllUserIds(userIds)
                    .build();

            GetUsersByIdsResponse response = authClient.getUsersByIds(request);

            Map<String, User> result = new HashMap<>();
            response.getUsersList().forEach(user -> {
                result.put(user.getId(), convertGrpcUserToUser(user));
            });

            return result;
        } catch (StatusRuntimeException e) {
            log.error("Failed to get users by IDs", e);
            return Collections.emptyMap();
        }
    }

    private User convertGrpcUserToUser(com.mmtext.common.grpc.User grpcUser) {
        // Convert gRPC User to domain User object
        User user = new User();
        user.setId(UUID.fromString(grpcUser.getId()));
        user.setEmail(grpcUser.getEmail());
        user.setFirstName(grpcUser.getFirstName());
        user.setLastName(grpcUser.getLastName());
        user.setFullName(grpcUser.getFullName());
        user.setAvatarUrl(grpcUser.getAvatarUrl());
        user.setActive(grpcUser.getActive());
        return user;
    }
}
```

### 3.2 Editor Service Client
```java
@GrpcClient("editor-service")
public interface EditorServiceGrpcClient extends EditorServiceGrpc.EditorServiceBlockingStub {
}

@Component
public class EditorServiceClient {

    @Autowired
    private EditorServiceGrpcClient editorClient;

    @Retry(name = "editorService")
    @CircuitBreaker(name = "editorService")
    public Optional<Document> getDocument(String documentId, String requestorId) {
        try {
            GetDocumentRequest request = GetDocumentRequest.newBuilder()
                    .setDocumentId(documentId)
                    .setRequestorId(requestorId)
                    .build();

            GetDocumentResponse response = editorClient.getDocument(request);

            if (response.hasDocument()) {
                return Optional.of(convertGrpcDocumentToDocument(response.getDocument()));
            }
            return Optional.empty();
        } catch (StatusRuntimeException e) {
            log.error("Failed to get document: {}", documentId, e);
            return Optional.empty();
        }
    }

    public boolean documentExists(String documentId) {
        try {
            DocumentExistsRequest request = DocumentExistsRequest.newBuilder()
                    .setDocumentId(documentId)
                    .build();

            DocumentExistsResponse response = editorClient.documentExists(request);
            return response.getExists();
        } catch (StatusRuntimeException e) {
            log.error("Failed to check document existence: {}", documentId, e);
            return false;
        }
    }

    public Map<String, Document> getDocumentsByIds(List<String> documentIds, String requestorId) {
        try {
            GetDocumentsByIdsRequest request = GetDocumentsByIdsRequest.newBuilder()
                    .addAllDocumentIds(documentIds)
                    .setRequestorId(requestorId)
                    .build();

            GetDocumentsByIdsResponse response = editorClient.getDocumentsByIds(request);

            Map<String, Document> result = new HashMap<>();
            response.getDocumentsList().forEach(doc -> {
                result.put(doc.getDocId(), convertGrpcDocumentToDocument(doc));
            });

            return result;
        } catch (StatusRuntimeException e) {
            log.error("Failed to get documents by IDs", e);
            return Collections.emptyMap();
        }
    }

    private Document convertGrpcDocumentToDocument(com.mmtext.common.grpc.Document grpcDoc) {
        // Convert gRPC Document to domain Document object
        Document document = new Document();
        document.setId(UUID.fromString(grpcDoc.getId()));
        document.setDocId(grpcDoc.getDocId());
        document.setTitle(grpcDoc.getTitle());
        document.setOwnerId(UUID.fromString(grpcDoc.getOwnerId()));
        document.setCreatedAt(convertTimestamp(grpcDoc.getCreatedAt()));
        document.setUpdatedAt(convertTimestamp(grpcDoc.getUpdatedAt()));
        return document;
    }

    private Instant convertTimestamp(Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }
}
```

## Phase 4: Service Integration

### 4.1 Update DocumentSharingService
```java
@Service
@Transactional
public class DocumentSharingService {

    @Autowired
    private DocumentSharingRepository sharingRepository;

    @Autowired
    private AuthServiceClient authServiceClient;

    @Autowired
    private EditorServiceClient editorServiceClient;

    @Autowired
    private EmailService emailService;

    @Autowired
    private AuditService auditService;

    public ShareResult shareDocument(String documentId, List<ShareRecipient> recipients, String sharedBy) {
        // Validate document exists
        Optional<Document> documentOpt = editorServiceClient.getDocument(documentId, sharedBy);
        if (documentOpt.isEmpty()) {
            throw new ResourceNotFoundException("Document not found: " + documentId);
        }

        Document document = documentOpt.get();

        // Get user information for all recipients
        List<String> emails = recipients.stream()
                .map(ShareRecipient::getEmail)
                .collect(Collectors.toList());

        Map<String, User> userMap = new HashMap<>();
        for (String email : emails) {
            authServiceClient.getUserByEmail(email)
                    .ifPresent(user -> userMap.put(email, user));
        }

        // Create sharing records
        List<DocumentPermission> permissions = new ArrayList<>();
        for (ShareRecipient recipient : recipients) {
            User user = userMap.get(recipient.getEmail());
            if (user == null) {
                continue; // Skip invalid users
            }

            DocumentPermission permission = new DocumentPermission();
            permission.setDocumentId(documentId);
            permission.setUserId(user.getId().toString());
            permission.setPermissionLevel(recipient.getPermission());
            permission.setSharedBy(sharedBy);
            permission.setSharedAt(Instant.now());

            permissions.add(permission);

            // Send email notification
            emailService.sendDocumentSharedEmail(
                user.getEmail(),
                user.getFullName(),
                document.getTitle(),
                recipient.getPermission()
            );

            // Log audit
            auditService.logShareDocument(documentId, user.getId().toString(), sharedBy, recipient.getPermission());
        }

        // Save all permissions
        sharingRepository.saveAll(permissions);

        return new ShareResult(
            documentId,
            permissions.size(),
            "Document shared successfully"
        );
    }

    // Other methods...
}
```

### 4.2 Update AccessRequestService
```java
@Service
@Transactional
public class AccessRequestService {

    @Autowired
    private AccessRequestRepository accessRequestRepository;

    @Autowired
    private EditorServiceClient editorServiceClient;

    @Autowired
    private AuthServiceClient authServiceClient;

    @Autowired
    private EmailService emailService;

    @Autowired
    private AuditService auditService;

    public AccessRequestResponse requestAccess(RequestAccessDto request) {
        String documentId = request.getDocumentId();
        String requestorEmail = request.getRequestorEmail();
        String message = request.getMessage();

        // Validate document exists
        if (!editorServiceClient.documentExists(documentId)) {
            throw new ResourceNotFoundException("Document not found: " + documentId);
        }

        // Get document info
        Optional<Document> documentOpt = editorServiceClient.getDocument(documentId, requestorEmail);
        if (documentOpt.isEmpty()) {
            throw new ResourceNotFoundException("Cannot access document: " + documentId);
        }

        Document document = documentOpt.get();

        // Get requestor user info
        Optional<User> requestorOpt = authServiceClient.getUserByEmail(requestorEmail);
        if (requestorOpt.isEmpty()) {
            throw new ResourceNotFoundException("User not found: " + requestorEmail);
        }

        User requestor = requestorOpt.get();

        // Check if request already exists
        Optional<AccessRequest> existingRequest = accessRequestRepository
                .findByDocumentIdAndRequestorIdAndStatus(
                    documentId,
                    requestor.getId().toString(),
                    RequestStatus.PENDING
                );

        if (existingRequest.isPresent()) {
            return new AccessRequestResponse(
                existingRequest.get().getId(),
                "Access request already exists",
                RequestStatus.PENDING
            );
        }

        // Create access request
        AccessRequest accessRequest = new AccessRequest();
        accessRequest.setDocumentId(documentId);
        accessRequest.setDocumentTitle(document.getTitle());
        accessRequest.setRequestorId(requestor.getId().toString());
        accessRequest.setRequestorEmail(requestorEmail);
        accessRequest.setMessage(message);
        accessRequest.setStatus(RequestStatus.PENDING);
        accessRequest.setRequestedAt(Instant.now());

        AccessRequest savedRequest = accessRequestRepository.save(accessRequest);

        // Get document owner info
        Optional<User> ownerOpt = authServiceClient.getUserById(document.getOwnerId().toString());
        if (ownerOpt.isPresent()) {
            User owner = ownerOpt.get();

            // Send email to document owner
            emailService.sendAccessRequestEmail(
                owner.getEmail(),
                owner.getFullName(),
                requestor.getFullName(),
                document.getTitle(),
                message
            );
        }

        // Log audit
        auditService.logAccessRequest(documentId, requestor.getId().toString(), RequestStatus.PENDING);

        return new AccessRequestResponse(
            savedRequest.getId(),
            "Access request submitted successfully",
            RequestStatus.PENDING
        );
    }

    // Other methods...
}
```

## Phase 5: Error Handling and Resilience

### 5.1 Circuit Breaker Configuration
```yaml
# Add to application.yml
resilience4j:
  circuitbreaker:
    instances:
      authService:
        registerHealthIndicator: true
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
        waitDurationInOpenState: 5s
        failureRateThreshold: 50
      editorService:
        registerHealthIndicator: true
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
        waitDurationInOpenState: 5s
        failureRateThreshold: 50

  retry:
    instances:
      authService:
        maxAttempts: 3
        waitDuration: 1s
        retryExceptions:
          - io.grpc.StatusRuntimeException
      editorService:
        maxAttempts: 3
        waitDuration: 1s
        retryExceptions:
          - io.grpc.StatusRuntimeException
```

### 5.2 Exception Handling
```java
@ControllerAdvice
public class GrpcExceptionHandler {

    @ExceptionHandler(StatusRuntimeException.class)
    public ResponseEntity<ErrorResponse> handleGrpcException(StatusRuntimeException e) {
        ErrorResponse error = new ErrorResponse();
        error.setMessage(e.getMessage());
        error.setStatusCode(statusFromGrpcStatus(e.getStatus()));

        return ResponseEntity.status(error.getStatusCode()).body(error);
    }

    private int statusFromGrpcStatus(Status status) {
        switch (status.getCode()) {
            case NOT_FOUND:
                return HttpStatus.NOT_FOUND.value();
            case PERMISSION_DENIED:
                return HttpStatus.FORBIDDEN.value();
            case UNAUTHENTICATED:
                return HttpStatus.UNAUTHORIZED.value();
            case UNAVAILABLE:
                return HttpStatus.SERVICE_UNAVAILABLE.value();
            default:
                return HttpStatus.INTERNAL_SERVER_ERROR.value();
        }
    }
}
```

## Phase 6: Server Implementations (Required in Other Modules)

### 6.1 Auth Server gRPC Implementation
```java
// To be implemented in auth-server module
@GrpcService
public class AuthServiceGrpcImpl extends AuthServiceGrpc.AuthServiceImplBase {

    @Autowired
    private UserService userService;

    @Override
    public void getUserById(GetUserByIdRequest request,
                           StreamObserver<GetUserByIdResponse> responseObserver) {
        try {
            Optional<User> userOpt = userService.findById(UUID.fromString(request.getUserId()));

            GetUserByIdResponse.Builder responseBuilder = GetUserByIdResponse.newBuilder();

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                com.mmtext.common.grpc.User grpcUser = convertUserToGrpc(user);
                responseBuilder.setUser(grpcUser);
            } else {
                responseBuilder.setError("User not found");
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    // Implement other methods...
}
```

### 6.2 Editor Server gRPC Implementation
```java
// To be implemented in editor-server-snapshot module
@GrpcService
public class EditorServiceGrpcImpl extends EditorServiceGrpc.EditorServiceImplBase {

    @Autowired
    private DocumentService documentService;

    @Override
    public void getDocument(GetDocumentRequest request,
                           StreamObserver<GetDocumentResponse> responseObserver) {
        try {
            Optional<Document> documentOpt = documentService.getDocumentByDocId(request.getDocumentId());

            GetDocumentResponse.Builder responseBuilder = GetDocumentResponse.newBuilder();

            if (documentOpt.isPresent()) {
                Document document = documentOpt.get();
                com.mmtext.common.grpc.Document grpcDoc = convertDocumentToGrpc(document);
                responseBuilder.setDocument(grpcDoc);
            } else {
                responseBuilder.setError("Document not found");
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    // Implement other methods...
}
```

## Implementation Priorities

1. **High Priority**:
   - Create proto files
   - Set up gRPC dependencies and configuration
   - Implement clients in editor-server-share
   - Implement servers in auth-server and editor-server-snapshot

2. **Medium Priority**:
   - Add circuit breakers and retry logic
   - Implement proper error handling
   - Add logging and monitoring

3. **Low Priority**:
   - Implement streaming for real-time updates
   - Add gRPC health checks
   - Performance optimization

## Benefits of gRPC Implementation

1. **Performance**: 5-10x faster than REST
2. **Type Safety**: Compile-time type checking
3. **Code Generation**: Auto-generated client stubs
4. **Streaming**: Native bidirectional streaming support
5. **Efficiency**: Binary serialization
6. **Modern**: HTTP/2 with multiplexing

This plan provides a comprehensive gRPC implementation that will significantly improve inter-service communication performance and reliability.
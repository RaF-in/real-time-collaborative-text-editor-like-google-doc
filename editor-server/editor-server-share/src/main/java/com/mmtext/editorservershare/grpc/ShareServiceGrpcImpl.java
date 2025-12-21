package com.mmtext.editorservershare.grpc;

import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import com.mmtext.editorservershare.model.DocumentPermission;
import com.mmtext.editorservershare.repo.DocumentPermissionRepository;
import com.mmtext.editorservershare.service.AuditService;
import com.mmtext.share.grpc.ShareServiceGrpc;
import com.mmtext.share.grpc.ShareServiceProto;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.PreDestroy;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


/**
 * gRPC service implementation for Share Service
 * Handles document permission creation and management
 */
@Service
@GrpcService
public class ShareServiceGrpcImpl extends ShareServiceGrpc.ShareServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(ShareServiceGrpcImpl.class);
    private static final String DOCUMENT_ID_NAMESPACE = "550e8400-e29b-41d4-a716-446655440000";

    @Autowired
    private DocumentPermissionRepository permissionRepository;

    @Autowired
    private AuditService auditService;

    // Constructor to check if bean is created
    public ShareServiceGrpcImpl() {
        log.info("ShareServiceGrpcImpl bean created successfully!");
    }

    /**
     * Create initial permissions for a newly created document
     * This is called synchronously during document creation
     */
    @Override
    @Transactional
    public void createDocumentPermissions(
            ShareServiceProto.CreateDocumentPermissionsRequest request,
            StreamObserver<ShareServiceProto.CreateDocumentPermissionsResponse> responseObserver) {

        String documentId = request.getDocumentId();
        String ownerId = request.getOwnerId();

        log.info("Creating document permissions - Document: {}, Owner: {}", documentId, ownerId);

        try {
            // Validate the request
            validateCreatePermissionsRequest(request);

            // Convert document ID to UUID
            UUID ownerUuid = resolveUserUuid(ownerId);

            // Check if permissions already exist (idempotent operation)
            if (permissionRepository.existsByDocumentIdAndUserId(documentId, ownerUuid)) {
                log.info("Permissions already exist for document: {}, owner: {}", documentId, ownerId);
                DocumentPermission existingPermission = permissionRepository
                        .findByDocumentIdAndUserId(documentId, ownerUuid)
                        .orElseThrow(() -> new RuntimeException("Permission exists but cannot be retrieved"));

                ShareServiceProto.CreateDocumentPermissionsResponse response =
                    ShareServiceProto.CreateDocumentPermissionsResponse.newBuilder()
                        .setSuccess(true)
                        .setDocumentId(documentId)
                        .setPermissionId(existingPermission.getId().toString())
                        .setGrantedPermission(convertPermissionLevel(existingPermission.getPermissionLevel()))
                        .setGrantedAt(Timestamp.newBuilder()
                            .setSeconds(existingPermission.getGrantedAt().getEpochSecond())
                            .setNanos(existingPermission.getGrantedAt().getNano())
                            .build())
                        .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Create new owner permission
            DocumentPermission ownerPermission = new DocumentPermission();
            ownerPermission.setDocumentId(documentId);
            ownerPermission.setUserId(ownerUuid);
            ownerPermission.setPermissionLevel(com.mmtext.editorservershare.enums.PermissionLevel.OWNER);
            ownerPermission.setGrantedBy(ownerUuid); // Owner grants permission to themselves
            ownerPermission.setGrantedAt(Instant.now()); // Set grantedAt explicitly

            // Save the permission
            DocumentPermission savedPermission = permissionRepository.save(ownerPermission);

            // Record the audit event (disabled due to schema mismatch)
            // TODO: Fix audit service to handle ip_address inet type properly
            /*
            if (auditService != null) {
                auditService.logPermissionCreated(
                        documentId,
                        ownerUuid,
                        com.mmtext.editorservershare.enums.PermissionLevel.OWNER,
                        "Initial owner permission created during document creation"
                );
            }
            */

            log.info("Successfully created owner permission for document: {}, permissionId: {}",
                    documentId, savedPermission.getId());

            // Build response
            Instant grantedAt = savedPermission.getGrantedAt();
            if (grantedAt == null) {
                grantedAt = Instant.now(); // Fallback if somehow still null
            }

            ShareServiceProto.CreateDocumentPermissionsResponse response =
                ShareServiceProto.CreateDocumentPermissionsResponse.newBuilder()
                    .setSuccess(true)
                    .setDocumentId(documentId)
                    .setPermissionId(savedPermission.getId().toString())
                    .setGrantedPermission(ShareServiceProto.PermissionLevel.OWNER)
                    .setGrantedAt(Timestamp.newBuilder()
                        .setSeconds(grantedAt.getEpochSecond())
                        .setNanos(grantedAt.getNano())
                            .build())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Failed to create permissions for document {}: {}", documentId, e.getMessage(), e);
            sendErrorResponse(responseObserver, documentId,
                "Internal server error: " + e.getMessage(), Status.INTERNAL);
        }
    }

    /**
     * Check if permissions exist for a document
     */
    @Override
    public void checkPermissionsExist(
            ShareServiceProto.CheckPermissionsExistRequest request,
            StreamObserver<ShareServiceProto.CheckPermissionsExistResponse> responseObserver) {

        String documentId = request.getDocumentId();

        try {
            if (documentId == null || documentId.trim().isEmpty()) {
                throw new IllegalArgumentException("Document ID cannot be null or empty");
            }


            // Check if any permissions exist for this document
            boolean exists = permissionRepository.existsByDocumentId(documentId);
            long permissionCount = 0;
            ShareServiceProto.PermissionLevel highestPermission = ShareServiceProto.PermissionLevel.UNKNOWN;

            if (exists) {
                permissionCount = permissionRepository.countByDocumentId(documentId);

                // Find the highest permission level
                List<DocumentPermission> permissions = permissionRepository.findByDocumentId(documentId);
                for (DocumentPermission permission : permissions) {
                    ShareServiceProto.PermissionLevel currentPermission = convertPermissionLevel(permission.getPermissionLevel());
                    if (currentPermission.getNumber() > highestPermission.getNumber()) {
                        highestPermission = currentPermission;
                    }
                }
            }

            ShareServiceProto.CheckPermissionsExistResponse response =
                ShareServiceProto.CheckPermissionsExistResponse.newBuilder()
                    .setExists(exists)
                    .setDocumentId(documentId)
                    .setHighestPermission(highestPermission)
                    .setPermissionCount((int) permissionCount)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error checking permissions for document {}: {}", documentId, e.getMessage(), e);
            responseObserver.onError(new StatusRuntimeException(
                Status.INTERNAL.withDescription("Error checking permissions: " + e.getMessage())));
        }
    }

    /**
     * Get user permissions for a document
     */
    @Override
    public void getUserPermissions(
            ShareServiceProto.GetUserPermissionsRequest request,
            StreamObserver<ShareServiceProto.GetUserPermissionsResponse> responseObserver) {

        String documentId = request.getDocumentId();
        String userId = request.getUserId();

        try {
            if (documentId == null || documentId.trim().isEmpty()) {
                throw new IllegalArgumentException("Document ID cannot be null or empty");
            }
            if (userId == null || userId.trim().isEmpty()) {
                throw new IllegalArgumentException("User ID cannot be null or empty");
            }


            UUID userUuid = resolveUserUuid(userId);

            Optional<DocumentPermission> permissionOpt = permissionRepository
                    .findByDocumentIdAndUserId(documentId, userUuid);

            ShareServiceProto.GetUserPermissionsResponse.Builder responseBuilder =
                ShareServiceProto.GetUserPermissionsResponse.newBuilder()
                    .setDocumentId(documentId)
                    .setUserId(userId)
                    .setHasPermission(false);

            if (permissionOpt.isPresent()) {
                DocumentPermission permission = permissionOpt.get();
                com.mmtext.editorservershare.enums.PermissionLevel permissionLevel = permission.getPermissionLevel();

                responseBuilder.setHasPermission(true)
                    .setPermissionLevel(convertPermissionLevel(permissionLevel))
                    .setCanEdit(permissionLevel.canEdit())
                    .setCanShare(permissionLevel.canShare())
                    .setCanManage(permissionLevel.canManagePermissions())
                    .setGrantedAt(Timestamp.newBuilder()
                        .setSeconds(permission.getGrantedAt().getEpochSecond())
                        .setNanos(permission.getGrantedAt().getNano())
                        .build());

                if (permission.getGrantedBy() != null) {
                    responseBuilder.setGrantedBy(permission.getGrantedBy().toString());
                }
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error getting user permissions for document {} user {}: {}",
                    documentId, userId, e.getMessage(), e);
            responseObserver.onError(new StatusRuntimeException(
                Status.INTERNAL.withDescription("Error getting user permissions: " + e.getMessage())));
        }
    }

    /**
     * Health check endpoint
     */
    @Override
    public void healthCheck(
            Empty request,
            StreamObserver<ShareServiceProto.HealthCheckResponse> responseObserver) {

        try {
            ShareServiceProto.HealthCheckResponse response =
                ShareServiceProto.HealthCheckResponse.newBuilder()
                    .setHealthy(true)
                    .setServiceName("ShareService")
                    .setVersion("1.0.0")
                    .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(Instant.now().getEpochSecond())
                        .setNanos(Instant.now().getNano())
                        .build())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Health check failed: {}", e.getMessage(), e);
            responseObserver.onError(new StatusRuntimeException(
                Status.INTERNAL.withDescription("Health check failed: " + e.getMessage())));
        }
    }

    /**
     * Send error response
     */
    private void sendErrorResponse(StreamObserver<ShareServiceProto.CreateDocumentPermissionsResponse> responseObserver,
                                 String documentId, String errorMessage, Status status) {
        ShareServiceProto.CreateDocumentPermissionsResponse response =
            ShareServiceProto.CreateDocumentPermissionsResponse.newBuilder()
                .setSuccess(false)
                .setDocumentId(documentId)
                .setErrorMessage(errorMessage)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    /**
     * Validate create permissions request
     */
    private void validateCreatePermissionsRequest(ShareServiceProto.CreateDocumentPermissionsRequest request) {
        if (request.getDocumentId() == null || request.getDocumentId().trim().isEmpty()) {
            throw new IllegalArgumentException("Document ID cannot be null or empty");
        }
        if (request.getOwnerId() == null || request.getOwnerId().trim().isEmpty()) {
            throw new IllegalArgumentException("Owner ID cannot be null or empty");
        }
        if (request.getCreatedAt() == null) {
            throw new IllegalArgumentException("Created timestamp cannot be null");
        }
    }

    /**
     * Convert document ID string to UUID using consistent namespace approach
     * This matches the approach used in SharingController
     */
    private UUID convertDocumentIdToUuid(String documentId) {
        if (documentId == null || documentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Document ID cannot be null or empty");
        }
        UUID namespace = UUID.fromString(DOCUMENT_ID_NAMESPACE);
        return UUID.nameUUIDFromBytes((namespace + documentId).getBytes());
    }

    /**
     * Resolve user ID to UUID
     * In a real implementation, this might call Auth service to resolve user ID
     * For now, we assume the string is a valid UUID string
     */
    private UUID resolveUserUuid(String userId) {
        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            // If not a valid UUID, create one deterministically
            return convertDocumentIdToUuid("user-" + userId);
        }
    }

    /**
     * Convert PermissionLevel enum
     */
    private ShareServiceProto.PermissionLevel convertPermissionLevel(
            com.mmtext.editorservershare.enums.PermissionLevel level) {
        switch (level) {
            case OWNER:
                return ShareServiceProto.PermissionLevel.OWNER;
            case EDITOR:
                return ShareServiceProto.PermissionLevel.EDITOR;
            case VIEWER:
                return ShareServiceProto.PermissionLevel.VIEWER;
            default:
                return ShareServiceProto.PermissionLevel.UNKNOWN;
        }
    }

    /**
     * Comparator for PermissionLevel
     */
    private static final java.util.Comparator<com.mmtext.editorservershare.enums.PermissionLevel> comparator =
        Comparator.comparing(com.mmtext.editorservershare.enums.PermissionLevel::getPriority);
}
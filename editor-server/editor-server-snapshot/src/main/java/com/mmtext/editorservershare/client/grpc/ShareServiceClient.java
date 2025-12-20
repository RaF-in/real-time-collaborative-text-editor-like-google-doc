package com.mmtext.editorservershare.client.grpc;

import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;

import com.mmtext.editorserversnapshot.model.Document;
import com.mmtext.share.grpc.ShareServiceGrpc;
import com.mmtext.share.grpc.ShareServiceProto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Production-grade gRPC client for Share Service
 * Handles synchronous permission creation with resilience patterns
 */
@Component
public class ShareServiceClient {

    private static final Logger log = LoggerFactory.getLogger(ShareServiceClient.class);

    private final ManagedChannel shareServiceChannel;
    private final ShareServiceGrpc.ShareServiceBlockingStub shareServiceStub;
    private final ShareServiceGrpc.ShareServiceFutureStub shareServiceFutureStub;

    public ShareServiceClient(
            @Value("${grpc.share.service.host:editor-server-share}") String host,
            @Value("${grpc.share.service.port:9092}") int port,
            @Value("${grpc.share.service.timeout:5}") int timeoutSeconds) {

        log.info("Creating ShareService gRPC client for {}:{}", host, port);

        this.shareServiceChannel = io.grpc.ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .keepAliveTime(30, TimeUnit.SECONDS)
                .keepAliveTimeout(5, TimeUnit.SECONDS)
                .keepAliveWithoutCalls(true)
                .maxInboundMessageSize(10 * 1024 * 1024) // 10MB
                .build();

        this.shareServiceStub = ShareServiceGrpc.newBlockingStub(shareServiceChannel)
                .withDeadlineAfter(timeoutSeconds, TimeUnit.SECONDS);

        this.shareServiceFutureStub = ShareServiceGrpc.newFutureStub(shareServiceChannel)
                .withDeadlineAfter(timeoutSeconds, TimeUnit.SECONDS);

        log.info("ShareService gRPC client created successfully");
    }

    /**
     * Create document permissions synchronously
     * This is called during document creation to ensure permissions are created atomically
     */
    @CircuitBreaker(name = "shareService", fallbackMethod = "createDocumentPermissionsFallback")
    @Retry(name = "shareService")
    public ShareServiceProto.CreateDocumentPermissionsResponse createDocumentPermissions(Document document) {
        if (document == null) {
            throw new IllegalArgumentException("Document cannot be null");
        }

        String documentId = document.getDocId();
        log.info("Calling Share Service to create permissions for document: {}", documentId);

        try {
            Timestamp createdAt = Timestamp.newBuilder()
                    .setSeconds(document.getCreatedAt().getEpochSecond())
                    .setNanos(document.getCreatedAt().getNano())
                    .build();

            ShareServiceProto.CreateDocumentPermissionsRequest request =
                    ShareServiceProto.CreateDocumentPermissionsRequest.newBuilder()
                            .setDocumentId(documentId)
                            .setDocumentTitle(document.getTitle() != null ? document.getTitle() : "Untitled Document")
                            .setOwnerId(document.getOwnerId())
                            .setCreatedAt(createdAt)
                            .setAllowAccessRequests(document.getAllowAccessRequests() != null ?
                                    document.getAllowAccessRequests() : true)
                            .build();

            log.debug("Sending CreateDocumentPermissions request: documentId={}, ownerId={}",
                    documentId, document.getOwnerId());

            ShareServiceProto.CreateDocumentPermissionsResponse response =
                    shareServiceStub.createDocumentPermissions(request);

            if (response.getSuccess()) {
                log.info("Successfully created permissions for document: {}, permissionId: {}",
                        documentId, response.getPermissionId());
            } else {
                log.error("Failed to create permissions for document: {}, error: {}",
                        documentId, response.getErrorMessage());
                throw new ShareServiceException("Permission creation failed: " + response.getErrorMessage());
            }

            return response;

        } catch (StatusRuntimeException e) {
            Status.Code code = e.getStatus().getCode();
            String description = e.getStatus().getDescription();

            log.error("gRPC error creating permissions for document {}: {} - {}",
                    documentId, code, description, e);

            // Handle specific gRPC errors appropriately
            switch (code) {
                case ALREADY_EXISTS:
                    throw new ShareServiceException("Permissions already exist for document: " + documentId);
                case DEADLINE_EXCEEDED:
                    throw new ShareServiceException("Timeout creating permissions for document: " + documentId);
                case UNAVAILABLE:
                    throw new ShareServiceException("Share service unavailable");
                case PERMISSION_DENIED:
                    throw new ShareServiceException("Permission denied creating permissions for document: " + documentId);
                default:
                    throw new ShareServiceException("gRPC error creating permissions: " + description);
            }

        } catch (Exception e) {
            log.error("Unexpected error creating permissions for document {}: {}", documentId, e.getMessage(), e);
            throw new ShareServiceException("Unexpected error creating permissions: " + e.getMessage(), e);
        }
    }

    /**
     * Fallback method for circuit breaker
     */
    public ShareServiceProto.CreateDocumentPermissionsResponse createDocumentPermissionsFallback(
            Document document, Exception e) {
        String documentId = document.getDocId();
        log.warn("Circuit breaker triggered for ShareService - documentId: {}", documentId);

        return ShareServiceProto.CreateDocumentPermissionsResponse.newBuilder()
                .setSuccess(false)
                .setDocumentId(documentId)
                .setErrorMessage("Share service unavailable - circuit breaker open: " + e.getMessage())
                .build();
    }

    /**
     * Check if permissions exist for a document
     * Used for idempotency checks
     */
    @CircuitBreaker(name = "shareService")
    @Retry(name = "shareService")
    public boolean checkPermissionsExist(String documentId) {
        if (documentId == null || documentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Document ID cannot be null or empty");
        }

        log.debug("Checking if permissions exist for document: {}", documentId);

        try {
            ShareServiceProto.CheckPermissionsExistRequest request =
                    ShareServiceProto.CheckPermissionsExistRequest.newBuilder()
                            .setDocumentId(documentId)
                            .build();

            ShareServiceProto.CheckPermissionsExistResponse response =
                    shareServiceStub.checkPermissionsExist(request);

            boolean exists = response.getExists();
            log.debug("Permissions check for document {}: {}", documentId, exists ? "exist" : "do not exist");

            return exists;

        } catch (StatusRuntimeException e) {
            log.warn("Error checking permissions for document {}: {} - {}",
                    documentId, e.getStatus().getCode(), e.getStatus().getDescription());
            // For permissions check, we assume they don't exist on error to avoid blocking document creation
            return false;
        }
    }

    /**
     * Get user permissions for a document
     */
    @CircuitBreaker(name = "shareService")
    @Retry(name = "shareService")
    public ShareServiceProto.GetUserPermissionsResponse getUserPermissions(String documentId, String userId) {
        if (documentId == null || documentId.trim().isEmpty() ||
                userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("Document ID and User ID cannot be null or empty");
        }

        log.debug("Getting permissions for document: {}, user: {}", documentId, userId);

        try {
            ShareServiceProto.GetUserPermissionsRequest request =
                    ShareServiceProto.GetUserPermissionsRequest.newBuilder()
                            .setDocumentId(documentId)
                            .setUserId(userId)
                            .build();

            return shareServiceStub.getUserPermissions(request);

        } catch (StatusRuntimeException e) {
            log.error("Error getting user permissions for document {} user {}: {} - {}",
                    documentId, userId, e.getStatus().getCode(), e.getStatus().getDescription());
            throw new ShareServiceException("Error getting user permissions: " + e.getStatus().getDescription(), e);
        }
    }

    /**
     * Health check for Share Service
     */
    @CircuitBreaker(name = "shareServiceHealth")
    public boolean checkServiceHealth() {
        try {
            ShareServiceProto.HealthCheckResponse response =
                    shareServiceStub.healthCheck(Empty.newBuilder().build());

            boolean healthy = response.getHealthy();
            log.debug("Share Service health check: {}", healthy ? "healthy" : "unhealthy");
            return healthy;

        } catch (Exception e) {
            log.warn("Share Service health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get service information
     */
    public String getServiceInfo() {
        try {
            ShareServiceProto.HealthCheckResponse response =
                    shareServiceStub.healthCheck(Empty.newBuilder().build());

            return String.format("Service: %s, Version: %s, Status: %s",
                    response.getServiceName(), response.getVersion(),
                    response.getHealthy() ? "Healthy" : "Unhealthy");

        } catch (Exception e) {
            return "Share Service unavailable: " + e.getMessage();
        }
    }

    /**
     * Shutdown the gRPC channel
     */
    @PreDestroy
    public void shutdown() {
        if (shareServiceChannel != null && !shareServiceChannel.isShutdown()) {
            log.info("Shutting down ShareService gRPC client");

            try {
                shareServiceChannel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
                log.info("ShareService gRPC client shutdown completed");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("ShareService gRPC client shutdown interrupted");
                shareServiceChannel.shutdownNow();
            }
        }
    }

    /**
     * Custom exception for Share Service operations
     */
    public static class ShareServiceException extends RuntimeException {
        public ShareServiceException(String message) {
            super(message);
        }

        public ShareServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
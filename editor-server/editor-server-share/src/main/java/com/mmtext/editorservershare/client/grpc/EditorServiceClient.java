package com.mmtext.editorservershare.client.grpc;

import com.google.protobuf.Timestamp;
import com.mmtext.common.grpc.CommonProto;
import com.mmtext.editorservershare.domain.Document;
import com.mmtext.editorservershare.domain.DocumentInfo;
import com.mmtext.editor.grpc.EditorServiceGrpc;
import com.mmtext.editor.grpc.EditorServiceProto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * gRPC client for Editor Service
 */
@Component
public class EditorServiceClient {

    private static final Logger log = LoggerFactory.getLogger(EditorServiceClient.class);

    private final ManagedChannel editorServiceChannel;

    @Autowired
    public EditorServiceClient(ManagedChannel editorServiceChannel) {
        this.editorServiceChannel = editorServiceChannel;
    }

    /**
     * Get document information
     */
    @CircuitBreaker(name = "editorService")
    @Retry(name = "editorService")
    public Optional<Document> getDocument(String documentId, String requestorId) {
        try {
            EditorServiceGrpc.EditorServiceBlockingStub stub = EditorServiceGrpc.newBlockingStub(editorServiceChannel);

            EditorServiceProto.GetDocumentRequest request = EditorServiceProto.GetDocumentRequest.newBuilder()
                    .setDocumentId(documentId)
                    .setRequestorId(requestorId)
                    .build();

            EditorServiceProto.GetDocumentResponse response = stub.getDocument(request);

            if (response.hasDocument()) {
                return Optional.of(convertGrpcDocumentToDomain(response.getDocument()));
            }

            if (response.hasError()) {
                log.warn("Error getting document {}: {}", documentId, response.getError());
            }

            return Optional.empty();
        } catch (StatusRuntimeException e) {
            log.error("Failed to get document: {}", documentId, e);
            return Optional.empty();
        }
    }

    /**
     * Get document basic info (for sharing and access requests)
     */
    @CircuitBreaker(name = "editorService")
    @Retry(name = "editorService")
    public DocumentInfo getDocumentInfo(String documentId) {
        try {
            EditorServiceGrpc.EditorServiceBlockingStub stub = EditorServiceGrpc.newBlockingStub(editorServiceChannel);

            EditorServiceProto.GetDocumentInfoRequest request = EditorServiceProto.GetDocumentInfoRequest.newBuilder()
                    .setDocumentId(documentId)
                    .build();

            EditorServiceProto.GetDocumentInfoResponse response = stub.getDocumentInfo(request);

            if (response.hasDocumentInfo()) {
                EditorServiceProto.DocumentInfo grpcDocInfo = response.getDocumentInfo();
                return new DocumentInfo(
                    grpcDocInfo.getId(),
                    grpcDocInfo.getDocId(),
                    grpcDocInfo.getTitle(),
                    grpcDocInfo.getOwnerId(),
                    grpcDocInfo.getAllowAccessRequests(),
                    convertTimestampToInstant(grpcDocInfo.getCreatedAt()),
                    convertTimestampToInstant(grpcDocInfo.getUpdatedAt())
                );
            }

            if (response.hasError()) {
                log.error("Error getting document info {}: {}", documentId, response.getError());
                throw new RuntimeException(response.getError());
            }

            throw new RuntimeException("Unknown error occurred while fetching document info");
        } catch (StatusRuntimeException e) {
            log.error("Failed to get document info: {}", documentId, e);
            throw new RuntimeException("Failed to fetch document info: " + e.getMessage());
        }
    }

    /**
     * Check if document exists
     */
    @CircuitBreaker(name = "editorService")
    @Retry(name = "editorService")
    public boolean documentExists(String documentId) {
        try {
            EditorServiceGrpc.EditorServiceBlockingStub stub = EditorServiceGrpc.newBlockingStub(editorServiceChannel);

            EditorServiceProto.DocumentExistsRequest request = EditorServiceProto.DocumentExistsRequest.newBuilder()
                    .setDocumentId(documentId)
                    .build();

            EditorServiceProto.DocumentExistsResponse response = stub.documentExists(request);
            return response.getExists();
        } catch (StatusRuntimeException e) {
            log.error("Failed to check document existence: {}", documentId, e);
            return false;
        }
    }

    /**
     * Get document with access control information
     */
    @CircuitBreaker(name = "editorService")
    @Retry(name = "editorService")
    public Optional<DocumentWithAccess> getDocumentWithAccess(String documentId, String requestorId) {
        try {
            EditorServiceGrpc.EditorServiceBlockingStub stub = EditorServiceGrpc.newBlockingStub(editorServiceChannel);

            EditorServiceProto.GetDocumentWithAccessRequest request = EditorServiceProto.GetDocumentWithAccessRequest.newBuilder()
                    .setDocumentId(documentId)
                    .setRequestorId(requestorId)
                    .build();

            EditorServiceProto.GetDocumentWithAccessResponse response = stub.getDocumentWithAccess(request);

            if (response.hasDocument()) {
                EditorServiceProto.DocumentWithAccess grpcDoc = response.getDocument();
                return Optional.of(
                    new DocumentWithAccess(
                        convertGrpcDocumentToDomain(grpcDoc.getDocument()),
                        convertPermissionLevel(grpcDoc.getPermissionLevel()),
                        grpcDoc.getCanShare(),
                        grpcDoc.getCanEdit()
                    )
                );
            }

            if (response.hasError()) {
                log.warn("Error getting document with access {}: {}", documentId, response.getError());
            }

            return Optional.empty();
        } catch (StatusRuntimeException e) {
            log.error("Failed to get document with access: {}", documentId, e);
            return Optional.empty();
        }
    }

    /**
     * Update document sharing settings
     */
    @CircuitBreaker(name = "editorService")
    @Retry(name = "editorService")
    public boolean updateSharingSettings(String documentId, String ownerId, boolean allowAccessRequests, String updatedBy) {
        try {
            EditorServiceGrpc.EditorServiceBlockingStub stub = EditorServiceGrpc.newBlockingStub(editorServiceChannel);

            EditorServiceProto.UpdateSharingSettingsRequest request = EditorServiceProto.UpdateSharingSettingsRequest.newBuilder()
                    .setDocumentId(documentId)
                    .setOwnerId(ownerId)
                    .setAllowAccessRequests(allowAccessRequests)
                    .setUpdatedBy(updatedBy)
                    .build();

            CommonProto.Response response = stub.updateSharingSettings(request);
            return response.getSuccess();
        } catch (StatusRuntimeException e) {
            log.error("Failed to update sharing settings for document: {}", documentId, e);
            return false;
        }
    }

    /**
     * Get multiple documents by IDs
     */
    @CircuitBreaker(name = "editorService")
    @Retry(name = "editorService")
    public Map<String, Document> getDocumentsByIds(List<String> documentIds, String requestorId) {
        if (documentIds == null || documentIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            EditorServiceGrpc.EditorServiceBlockingStub stub = EditorServiceGrpc.newBlockingStub(editorServiceChannel);

            EditorServiceProto.GetDocumentsByIdsRequest request = EditorServiceProto.GetDocumentsByIdsRequest.newBuilder()
                    .addAllDocumentIds(documentIds)
                    .setRequestorId(requestorId)
                    .build();

            EditorServiceProto.GetDocumentsByIdsResponse response = stub.getDocumentsByIds(request);

            Map<String, Document> result = new HashMap<>();
            response.getDocumentsList().forEach(grpcDoc -> {
                Document doc = convertGrpcDocumentToDomain(grpcDoc);
                result.put(doc.getDocId(), doc);
            });

            // Log any errors for specific document IDs
            if (!response.getErrorsMap().isEmpty()) {
                response.getErrorsMap().forEach((docId, error) -> {
                    log.warn("Error getting document {}: {}", docId, error);
                });
            }

            return result;
        } catch (StatusRuntimeException e) {
            log.error("Failed to get documents by IDs", e);
            return Collections.emptyMap();
        }
    }

    /**
     * Convert gRPC Document to domain Document
     */
    private Document convertGrpcDocumentToDomain(CommonProto.Document grpcDoc) {
        return Document.builder()
                .id(UUID.fromString(grpcDoc.getId()))
                .docId(grpcDoc.getDocId())
                .title(grpcDoc.getTitle())
                .ownerId(UUID.fromString(grpcDoc.getOwnerId()))
                .createdAt(convertTimestampToInstant(grpcDoc.getCreatedAt()))
                .updatedAt(convertTimestampToInstant(grpcDoc.getUpdatedAt()))
                .allowAccessRequests(grpcDoc.getAllowAccessRequests())
                .build();
    }

    /**
     * Convert protobuf PermissionLevel to domain PermissionLevel
     */
    private PermissionLevel convertPermissionLevel(EditorServiceProto.PermissionLevel grpcPermissionLevel) {
        switch (grpcPermissionLevel) {
            case NONE:
                return PermissionLevel.NONE;
            case READ:
                return PermissionLevel.READ;
            case WRITE:
                return PermissionLevel.WRITE;
            case OWNER:
                return PermissionLevel.OWNER;
            default:
                return PermissionLevel.NONE;
        }
    }

    /**
     * Convert protobuf Timestamp to Instant
     */
    private Instant convertTimestampToInstant(Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }

    /**
     * Document with access control information
     */
    public static class DocumentWithAccess {
        private final Document document;
        private final PermissionLevel permissionLevel;
        private final boolean canShare;
        private final boolean canEdit;

        public DocumentWithAccess(Document document, PermissionLevel permissionLevel, boolean canShare, boolean canEdit) {
            this.document = document;
            this.permissionLevel = permissionLevel;
            this.canShare = canShare;
            this.canEdit = canEdit;
        }

        public Document getDocument() {
            return document;
        }

        public PermissionLevel getPermissionLevel() {
            return permissionLevel;
        }

        public boolean canShare() {
            return canShare;
        }

        public boolean canEdit() {
            return canEdit;
        }
    }

    /**
     * Permission levels for documents
     */
    public enum PermissionLevel {
        NONE,
        READ,
        WRITE,
        OWNER
    }
}
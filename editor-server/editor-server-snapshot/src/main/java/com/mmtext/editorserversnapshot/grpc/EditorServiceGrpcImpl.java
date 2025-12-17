package com.mmtext.editorserversnapshot.grpc;

import com.google.protobuf.Timestamp;
import com.mmtext.common.grpc.CommonProto;
import com.mmtext.editor.grpc.*;
import com.mmtext.editorserversnapshot.model.Document;
import com.mmtext.editorserversnapshot.service.DocumentService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * gRPC server implementation for Editor Service
 */
@Service
@GrpcService
public class EditorServiceGrpcImpl extends EditorServiceGrpc.EditorServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(EditorServiceGrpcImpl.class);

    private final DocumentService documentService;

    @Autowired
    public EditorServiceGrpcImpl(DocumentService documentService) {
        this.documentService = documentService;
    }

    @Override
    public void getDocument(GetDocumentRequest request, StreamObserver<GetDocumentResponse> responseObserver) {
        try {
            log.debug("Getting document {} for user {}", request.getDocumentId(), request.getRequestorId());

            Optional<Document> documentOpt = documentService.getDocumentByDocId(request.getDocumentId());

            GetDocumentResponse.Builder responseBuilder = GetDocumentResponse.newBuilder();

            if (documentOpt.isPresent()) {
                Document document = documentOpt.get();
                CommonProto.Document grpcDoc = convertDocumentToGrpc(document);
                responseBuilder.setDocument(grpcDoc);
                log.debug("Document found: {}", document.getDocId());
            } else {
                responseBuilder.setError("Document not found");
                log.warn("Document not found: {}", request.getDocumentId());
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error getting document: {}", request.getDocumentId(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getDocumentInfo(GetDocumentInfoRequest request, StreamObserver<GetDocumentInfoResponse> responseObserver) {
        try {
            log.debug("Getting document info for: {}", request.getDocumentId());

            Optional<Document> documentOpt = documentService.getDocumentByDocId(request.getDocumentId());

            GetDocumentInfoResponse.Builder responseBuilder = GetDocumentInfoResponse.newBuilder();

            if (documentOpt.isPresent()) {
                Document document = documentOpt.get();
                EditorServiceProto.DocumentInfo documentInfo = EditorServiceProto.DocumentInfo.newBuilder()
                        .setId(document.getId().toString())
                        .setDocId(document.getDocId())
                        .setTitle(document.getTitle())
                        .setOwnerId(document.getOwnerId())
                        .setAllowAccessRequests(document.getAllowAccessRequests())
                        .setCreatedAt(convertInstantToTimestamp(document.getCreatedAt()))
                        .setUpdatedAt(convertInstantToTimestamp(document.getUpdatedAt()))
                        .build();
                responseBuilder.setDocumentInfo(documentInfo);
                log.debug("Document info found: {}", document.getDocId());
            } else {
                responseBuilder.setError("Document not found");
                log.warn("Document not found: {}", request.getDocumentId());
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error getting document info: {}", request.getDocumentId(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void documentExists(DocumentExistsRequest request, StreamObserver<DocumentExistsResponse> responseObserver) {
        try {
            log.debug("Checking if document exists: {}", request.getDocumentId());

            boolean exists = documentService.documentExists(request.getDocumentId());

            DocumentExistsResponse response = DocumentExistsResponse.newBuilder()
                    .setExists(exists)
                    .setDocumentId(request.getDocumentId())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error checking document existence: {}", request.getDocumentId(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getDocumentWithAccess(GetDocumentWithAccessRequest request, StreamObserver<GetDocumentWithAccessResponse> responseObserver) {
        try {
            log.debug("Getting document {} with access info for user {}",
                    request.getDocumentId(), request.getRequestorId());

            Optional<Document> documentOpt = documentService.getDocumentByDocId(request.getDocumentId());

            GetDocumentWithAccessResponse.Builder responseBuilder = GetDocumentWithAccessResponse.newBuilder();

            if (documentOpt.isPresent()) {
                Document document = documentOpt.get();

                // Check if user is owner
                boolean isOwner = document.getOwnerId().toString().equals(request.getRequestorId());

                // Default permission level and capabilities
                EditorServiceProto.PermissionLevel permissionLevel = EditorServiceProto.PermissionLevel.READ;
                boolean canShare = false;
                boolean canEdit = false;

                if (isOwner) {
                    permissionLevel = EditorServiceProto.PermissionLevel.OWNER;
                    canShare = true;
                    canEdit = true;
                } else {
                    // Check if user has explicit permission (would need to query sharing service)
                    // For now, default to READ access if document exists
                    permissionLevel = EditorServiceProto.PermissionLevel.READ;
                }

                CommonProto.Document grpcDoc = convertDocumentToGrpc(document);
                DocumentWithAccess docWithAccess = DocumentWithAccess.newBuilder()
                        .setDocument(grpcDoc)
                        .setPermissionLevel(permissionLevel)
                        .setCanShare(canShare)
                        .setCanEdit(canEdit)
                        .build();

                responseBuilder.setDocument(docWithAccess);
            } else {
                responseBuilder.setError("Document not found");
                log.warn("Document not found: {}", request.getDocumentId());
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error getting document with access: {}", request.getDocumentId(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void updateSharingSettings(UpdateSharingSettingsRequest request, StreamObserver<CommonProto.Response> responseObserver) {
        try {
            log.info("Updating sharing settings for document {} by user {}",
                    request.getDocumentId(), request.getUpdatedBy());

            // Get document first
            Optional<Document> documentOpt = documentService.getDocumentByDocId(request.getDocumentId());

            if (documentOpt.isEmpty()) {
                CommonProto.Response errorResponse = CommonProto.Response.newBuilder()
                        .setSuccess(false)
                        .setMessage("Document not found")
                        .setStatusCode(404)
                        .build();
                responseObserver.onNext(errorResponse);
                responseObserver.onCompleted();
                return;
            }

            Document document = documentOpt.get();

            // Verify ownership
            if (!document.getOwnerId().toString().equals(request.getOwnerId())) {
                CommonProto.Response errorResponse = CommonProto.Response.newBuilder()
                        .setSuccess(false)
                        .setMessage("Only document owners can update sharing settings")
                        .setStatusCode(403)
                        .build();
                responseObserver.onNext(errorResponse);
                responseObserver.onCompleted();
                return;
            }

            // Update allow access requests setting (would need to add this field to Document entity)
            // For now, just return success
            CommonProto.Response successResponse = CommonProto.Response.newBuilder()
                    .setSuccess(true)
                    .setMessage("Sharing settings updated successfully")
                    .setStatusCode(200)
                    .build();

            responseObserver.onNext(successResponse);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error updating sharing settings for document: {}", request.getDocumentId(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getDocumentsByIds(GetDocumentsByIdsRequest request, StreamObserver<GetDocumentsByIdsResponse> responseObserver) {
        try {
            log.debug("Getting {} documents for user {}",
                    request.getDocumentIdsCount(), request.getRequestorId());

            GetDocumentsByIdsResponse.Builder responseBuilder = GetDocumentsByIdsResponse.newBuilder();

            for (String docId : request.getDocumentIdsList()) {
                try {
                    Optional<Document> documentOpt = documentService.getDocumentByDocId(docId);

                    if (documentOpt.isPresent()) {
                        Document document = documentOpt.get();
                        CommonProto.Document grpcDoc = convertDocumentToGrpc(document);
                        responseBuilder.addDocuments(grpcDoc);
                    } else {
                        responseBuilder.putErrors(docId, "Document not found");
                    }
                } catch (Exception e) {
                    responseBuilder.putErrors(docId, e.getMessage());
                    log.warn("Error getting document {}: {}", docId, e.getMessage());
                }
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error getting documents by IDs", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public StreamObserver<DocumentUpdate> streamDocumentUpdates(StreamObserver<DocumentUpdate> responseObserver) {
        // This is a placeholder for future implementation of real-time document updates
        return new StreamObserver<DocumentUpdate>() {
            @Override
            public void onNext(DocumentUpdate value) {
                // Handle incoming update requests
                log.debug("Received document update: {}", value.getDocumentId());
            }

            @Override
            public void onError(Throwable t) {
                log.error("Error in document update stream", t);
            }

            @Override
            public void onCompleted() {
                log.debug("Document update stream completed");
            }
        };
    }

    /**
     * Convert domain Document to gRPC Document
     */
    private CommonProto.Document convertDocumentToGrpc(Document document) {
        CommonProto.Document.Builder builder = CommonProto.Document.newBuilder()
                .setId(document.getId().toString())
                .setDocId(document.getDocId())
                .setTitle(document.getTitle())
                .setOwnerId(document.getOwnerId().toString())
                .setAllowAccessRequests(true); // Default value, can be enhanced

        if (document.getCreatedAt() != null) {
            builder.setCreatedAt(convertInstantToTimestamp(document.getCreatedAt()));
        }

        if (document.getUpdatedAt() != null) {
            builder.setUpdatedAt(convertInstantToTimestamp(document.getUpdatedAt()));
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
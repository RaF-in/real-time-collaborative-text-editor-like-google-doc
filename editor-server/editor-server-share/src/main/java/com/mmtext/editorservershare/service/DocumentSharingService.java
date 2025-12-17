package com.mmtext.editorservershare.service;

import com.mmtext.editorservershare.client.grpc.AuthServiceClient;
import com.mmtext.editorservershare.client.grpc.EditorServiceClient;
import com.mmtext.editorservershare.domain.User;
import com.mmtext.editorservershare.domain.Document;
import com.mmtext.editorservershare.dto.*;
import com.mmtext.editorservershare.enums.PermissionLevel;
import com.mmtext.editorservershare.model.DocumentPermission;
import com.mmtext.editorservershare.repo.DocumentPermissionRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DocumentSharingService {

    private static final Logger log = LoggerFactory.getLogger(DocumentSharingService.class);

    private final DocumentPermissionRepository permissionRepository;
    private final EmailService emailService;
    private final AuditService auditService;
    private final AuthServiceClient authServiceClient;
    private final EditorServiceClient editorServiceClient;

    @Autowired
    public DocumentSharingService(
            DocumentPermissionRepository permissionRepository,
            EmailService emailService,
            AuditService auditService,
            AuthServiceClient authServiceClient,
            EditorServiceClient editorServiceClient) {
        this.permissionRepository = permissionRepository;
        this.emailService = emailService;
        this.auditService = auditService;
        this.authServiceClient = authServiceClient;
        this.editorServiceClient = editorServiceClient;
    }

    /**
     * Share document with multiple people at once
     */
    @Transactional
    public ShareMultipleResponse shareWithMultiple(
            String documentId,
            ShareWithMultipleRequest request,
            String currentUserId,
            HttpServletRequest httpRequest) {

        log.info("Sharing document {} with {} recipients by user {}",
                documentId, request.getRecipients().size(), currentUserId);

        // Verify document exists and get its info
        Optional<Document> documentOpt = editorServiceClient.getDocument(documentId, currentUserId);
        if (documentOpt.isEmpty()) {
            throw new RuntimeException("Document not found: " + documentId);
        }

        Document document = documentOpt.get();

        // Verify current user is the owner or has share permission
        if (!document.getOwnerId().toString().equals(currentUserId)) {
            // Check if user has permission to share
            Optional<EditorServiceClient.DocumentWithAccess> docAccessOpt =
                editorServiceClient.getDocumentWithAccess(documentId, currentUserId);
            if (docAccessOpt.isEmpty() || !docAccessOpt.get().canShare()) {
                throw new RuntimeException("You don't have permission to share this document");
            }
        }

        // Get user information for all recipients
        List<String> emails = request.getRecipients().stream()
                .map(ShareRecipient::getEmail)
                .collect(Collectors.toList());

        Map<String, User> userMap = new HashMap<>();
        for (String email : emails) {
            authServiceClient.getUserByEmail(email)
                    .ifPresent(user -> userMap.put(email, user));
        }

        // Create sharing records
        List<DocumentPermission> permissions = new ArrayList<>();
        List<ShareResult> shareResults = new ArrayList<>();

        for (ShareRecipient recipient : request.getRecipients()) {
            User user = userMap.get(recipient.getEmail());
            if (user == null) {
                shareResults.add(new ShareResult(
                    recipient.getEmail(),
                    false,
                    "User not found"
                ));
                continue;
            }

            // Check if permission already exists
            Optional<DocumentPermission> existingPermission = permissionRepository
                    .findByDocumentIdAndUserId(documentId, user.getId().toString());

            if (existingPermission.isPresent()) {
                shareResults.add(new ShareResult(
                    recipient.getEmail(),
                    false,
                    "Document already shared with this user"
                ));
                continue;
            }

            // Create new permission
            DocumentPermission permission = new DocumentPermission();
            permission.setDocumentId(UUID.fromString(documentId));
            permission.setUserId(user.getId());
            permission.setPermissionLevel(convertPermissionLevel(recipient.getPermission()));
            permission.setGrantedBy(UUID.fromString(currentUserId));
            permission.setGrantedAt(Instant.now());

            permissions.add(permission);

            // Send email notification
            emailService.sendDocumentSharedEmail(
                user.getEmail(),
                user.getFullName(),
                document.getTitle(),
                recipient.getPermission()
            );

            // Log audit
            auditService.logShareDocument(documentId, user.getId().toString(), currentUserId, recipient.getPermission());

            shareResults.add(new ShareResult(
                recipient.getEmail(),
                true,
                "Document shared successfully"
            ));
        }

        // Save all permissions
        permissionRepository.saveAll(permissions);

        return new ShareMultipleResponse(
            documentId,
            shareResults,
            permissions.size(),
            "Document sharing completed"
        );
    }

    /**
     * Create a shareable link
     */
    @Transactional
    public ShareableLinkResponse createShareableLink(
            String documentId,
            CreateShareableLinkDto request,
            String currentUserId) {

        log.info("Creating shareable link for document {} by user {}", documentId, currentUserId);

        // Verify document exists
        Optional<Document> documentOpt = editorServiceClient.getDocument(documentId, currentUserId);
        if (documentOpt.isEmpty()) {
            throw new RuntimeException("Document not found: " + documentId);
        }

        Document document = documentOpt.get();

        // Verify ownership
        if (!document.getOwnerId().toString().equals(currentUserId)) {
            throw new RuntimeException("Only document owners can create shareable links");
        }

        // Create shareable link
        ShareableLinkService shareableLinkService = new ShareableLinkService(
            null, // repositories will be injected
            null,
            null
        );

        // This would need to be properly injected and implemented
        // For now, returning a placeholder
        return new ShareableLinkResponse(
            "link-id-placeholder",
            documentId,
            request.getPermissionLevel(),
            request.getExpiresAt(),
            Instant.now(),
            true
        );
    }

    /**
     * Get document access information
     */
    @Transactional(readOnly = true)
    public DocumentAccessInfoResponse getDocumentAccessInfo(String documentId, String currentUserId) {
        log.debug("Getting access info for document {} by user {}", documentId, currentUserId);

        // Verify document exists
        Optional<Document> documentOpt = editorServiceClient.getDocument(documentId, currentUserId);
        if (documentOpt.isEmpty()) {
            throw new RuntimeException("Document not found: " + documentId);
        }

        Document document = documentOpt.get();

        // Get all permissions for this document
        List<DocumentPermission> permissions = permissionRepository.findByDocumentId(documentId);

        // Get user information for all users with permissions
        Set<String> userIds = permissions.stream()
                .map(DocumentPermission::getUserId)
                .collect(Collectors.toSet());

        Map<String, User> userMap = authServiceClient.getUsersByIds(new ArrayList<>(userIds));

        // Build response
        List<DocumentPermissionResponse> permissionResponses = permissions.stream()
                .map(permission -> {
                    User user = userMap.get(permission.getUserId());
                    return new DocumentPermissionResponse(
                        permission.getId(),
                        permission.getDocumentId(),
                        user != null ? user.getId().toString() : permission.getUserId(),
                        user != null ? user.getFullName() : "Unknown User",
                        user != null ? user.getEmail() : "",
                        permission.getPermissionLevel(),
                        permission.getSharedBy(),
                        permission.getSharedAt()
                    );
                })
                .collect(Collectors.toList());

        return new DocumentAccessInfoResponse(
            documentId,
            document.getTitle(),
            document.getOwnerId().toString(),
            permissionResponses
        );
    }

    /**
     * Update user's permission for a document
     */
    @Transactional
    public DocumentPermissionResponse updatePermission(
            String documentId,
            String userId,
            UpdatePermissionDto request,
            String currentUserId) {

        log.info("Updating permission for document {} user {} to {} by user {}",
                documentId, userId, request.getPermissionLevel(), currentUserId);

        // Verify document exists and user has permission
        Optional<EditorServiceClient.DocumentWithAccess> docAccessOpt =
            editorServiceClient.getDocumentWithAccess(documentId, currentUserId);
        if (docAccessOpt.isEmpty() || !docAccessOpt.get().canShare()) {
            throw new RuntimeException("You don't have permission to modify sharing for this document");
        }

        // Get existing permission
        Optional<DocumentPermission> permissionOpt = permissionRepository
                .findByDocumentIdAndUserId(documentId, userId);

        if (permissionOpt.isEmpty()) {
            throw new RuntimeException("Permission not found for this user");
        }

        DocumentPermission permission = permissionOpt.get();
        permission.setPermissionLevel(request.getPermissionLevel());
        permission.setUpdatedBy(currentUserId);
        permission.setUpdatedAt(Instant.now());

        permissionRepository.save(permission);

        // Get user information
        Optional<User> userOpt = authServiceClient.getUserById(userId);
        String userName = userOpt.map(User::getFullName).orElse("Unknown User");

        // Log audit
        auditService.logPermissionChanged(documentId, userId, currentUserId, request.getPermissionLevel());

        return new DocumentPermissionResponse(
            permission.getId(),
            documentId,
            userId,
            userName,
            userOpt.map(User::getEmail).orElse(""),
            permission.getPermissionLevel(),
            permission.getSharedBy(),
            permission.getSharedAt()
        );
    }

    /**
     * Remove user's permission for a document
     */
    @Transactional
    public void removePermission(String documentId, String userId, String currentUserId) {
        log.info("Removing permission for document {} user {} by user {}", documentId, userId, currentUserId);

        // Verify document exists and user has permission
        Optional<EditorServiceClient.DocumentWithAccess> docAccessOpt =
            editorServiceClient.getDocumentWithAccess(documentId, currentUserId);
        if (docAccessOpt.isEmpty() || !docAccessOpt.get().canShare()) {
            throw new RuntimeException("You don't have permission to modify sharing for this document");
        }

        // Get and delete permission
        Optional<DocumentPermission> permissionOpt = permissionRepository
                .findByDocumentIdAndUserId(documentId, userId);

        if (permissionOpt.isEmpty()) {
            throw new RuntimeException("Permission not found for this user");
        }

        permissionRepository.delete(permissionOpt.get());

        // Log audit
        auditService.logPermissionRemoved(documentId, userId, currentUserId);
    }

    /**
     * Validate that current user can share the document
     */
    private void validateCanShare(String documentId, String currentUserId) {
        Optional<EditorServiceClient.DocumentWithAccess> docAccessOpt =
            editorServiceClient.getDocumentWithAccess(documentId, currentUserId);

        if (docAccessOpt.isEmpty()) {
            throw new RuntimeException("Document not found or access denied");
        }

        EditorServiceClient.DocumentWithAccess docAccess = docAccessOpt.get();
        if (!docAccess.canShare()) {
            throw new RuntimeException("You don't have permission to share this document");
        }
    }

    /**
     * Convert string permission level to enum
     */
    private PermissionLevel convertPermissionLevel(String permissionLevel) {
        switch (permissionLevel.toUpperCase()) {
            case "OWNER":
                return PermissionLevel.OWNER;
            case "EDITOR":
            case "WRITE":
                return PermissionLevel.EDITOR;
            case "VIEWER":
            case "READ":
                return PermissionLevel.VIEWER;
            default:
                return PermissionLevel.VIEWER;
        }
    }
}
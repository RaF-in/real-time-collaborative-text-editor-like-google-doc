package com.mmtext.editorservershare.service;

import com.mmtext.editorservershare.client.grpc.AuthServiceClient;
import com.mmtext.editorservershare.client.grpc.EditorServiceClient;
import com.mmtext.editorservershare.domain.DocumentInfo;
import com.mmtext.editorservershare.domain.User;
import com.mmtext.editorservershare.dto.*;
import com.mmtext.editorservershare.enums.PermissionLevel;
import com.mmtext.editorservershare.exception.AccessDeniedException;
import com.mmtext.editorservershare.exception.ResourceNotFoundException;
import com.mmtext.editorservershare.model.DocumentPermission;
import com.mmtext.editorservershare.repo.DocumentPermissionRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    public DocumentSharingService(DocumentPermissionRepository permissionRepository, EmailService emailService, AuditService auditService, AuthServiceClient authServiceClient, EditorServiceClient editorServiceClient) {
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
            UUID currentUserId,
            HttpServletRequest httpRequest) {

        log.info("Sharing document {} with {} recipients by user {}",
                documentId, request.getRecipients().size(), currentUserId);

        // Verify current user can share
        validateCanShare(documentId, currentUserId);

        // Get document info
        DocumentInfo documentInfo = editorServiceClient.getDocumentInfo(documentId);

        // Get sharer info
        User sharerInfo = authServiceClient.getUserById(currentUserId.toString())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUserId));

        List<ShareResult> results = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        for (ShareRecipient recipient : request.getRecipients()) {
            try {
                // Validate user exists
                User targetUser = authServiceClient.getUserByEmail(recipient.getEmail())
                        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + recipient.getEmail()));

                if (targetUser.getId().equals(currentUserId)) {
                    results.add(createFailureResult(recipient.getEmail(),
                            "Cannot share document with yourself"));
                    failureCount++;
                    continue;
                }

                // Check if permission already exists
                Optional<DocumentPermission> existing = permissionRepository
                        .findByDocumentIdAndUserId(documentId, targetUser.getId());

                DocumentPermission permission;
                boolean isNew = existing.isEmpty();

                if (existing.isPresent()) {
                    permission = existing.get();

                    // Don't allow changing owner permission
                    if (permission.getPermissionLevel() == PermissionLevel.OWNER) {
                        results.add(createFailureResult(recipient.getEmail(),
                                "Cannot modify owner permission"));
                        failureCount++;
                        continue;
                    }

                    // Update existing permission
                    permission.setPermissionLevel(recipient.getPermissionLevel());
                    permission.setGrantedBy(currentUserId);
                } else {
                    // Create new permission
                    permission = DocumentPermission.builder()
                            .documentId(documentId)
                            .userId(targetUser.getId())
                            .permissionLevel(recipient.getPermissionLevel())
                            .grantedBy(currentUserId)
                            .build();
                }

                permission = permissionRepository.save(permission);

                // Audit log
                auditService.logShare(documentId, currentUserId, targetUser.getId(),
                        recipient.getPermissionLevel(), httpRequest);

                // Send email invitation
                emailService.sendDocumentSharedEmail(
                        targetUser.getEmail(),
                        targetUser.getFullName(),
                        documentId,
                        documentInfo.getTitle(),
                        recipient.getPermissionLevel(),
                        sharerInfo.getFullName(),
                        request.getMessage()
                );

                // Build response
                DocumentPermissionResponse permissionResponse = mapToPermissionResponse(
                        permission, currentUserId, documentId, targetUser
                );

                results.add(ShareResult.builder()
                        .email(recipient.getEmail())
                        .success(true)
                        .message(isNew ? "Access granted" : "Permission updated")
                        .permission(permissionResponse)
                        .build());

                successCount++;

            } catch (Exception e) {
                log.error("Failed to share with {}: {}", recipient.getEmail(), e.getMessage(), e);
                results.add(createFailureResult(recipient.getEmail(), e.getMessage()));
                failureCount++;
            }
        }

        log.info("Share completed: {} successful, {} failed out of {} recipients",
                successCount, failureCount, request.getRecipients().size());

        return ShareMultipleResponse.builder()
                .totalRecipients(request.getRecipients().size())
                .successCount(successCount)
                .failureCount(failureCount)
                .results(results)
                .build();
    }

    /**
     * Get all permissions for a document
     */
    @Transactional(readOnly = true)
    public List<DocumentPermissionResponse> getDocumentPermissions(
            String documentId,
            UUID currentUserId) {

        // Verify user has access
        validateHasAccess(documentId, currentUserId, PermissionLevel.VIEWER);

        List<DocumentPermission> permissions = permissionRepository.findByDocumentId(documentId);

        // Enrich with user information
        return permissions.stream()
                .map(permission -> {
                    try {
                        User userInfo = authServiceClient.getUserById(permission.getUserId().toString())
                        .orElse(null);
                        return mapToPermissionResponse(permission, currentUserId, documentId, userInfo);
                    } catch (Exception e) {
                        log.warn("Failed to fetch user info for {}: {}", permission.getUserId(), e.getMessage());
                        return mapToPermissionResponse(permission, currentUserId, documentId, null);
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * Update a user's permission level
     */
    @Transactional
    public DocumentPermissionResponse updatePermission(
            String documentId,
            UpdatePermissionDto request,
            UUID currentUserId,
            HttpServletRequest httpRequest) {

        log.info("Updating permission for user {} on document {} by user {}",
                request.getUserId(), documentId, currentUserId);

        // Verify current user can manage permissions
        validateCanManagePermissions(documentId, currentUserId);

        // Get existing permission
        DocumentPermission permission = permissionRepository
                .findByDocumentIdAndUserId(documentId, request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Permission not found for user " + request.getUserId()));

        // Don't allow changing owner permission
        if (permission.getPermissionLevel() == PermissionLevel.OWNER) {
            throw new IllegalArgumentException("Cannot change owner permission");
        }

        // Don't allow changing to owner
        if (request.getPermissionLevel() == PermissionLevel.OWNER) {
            throw new IllegalArgumentException("Cannot grant owner permission");
        }

        PermissionLevel oldLevel = permission.getPermissionLevel();
        permission.setPermissionLevel(request.getPermissionLevel());
        permission.setGrantedBy(currentUserId);
        permission = permissionRepository.save(permission);

        // Audit log
        auditService.logPermissionChange(documentId, currentUserId, request.getUserId(),
                oldLevel, request.getPermissionLevel(), httpRequest);

        // Get user info for response
        User userInfo = authServiceClient.getUserById(request.getUserId().toString())
                .orElse(null);

        log.info("Permission updated successfully for user {} on document {}",
                request.getUserId(), documentId);

        return mapToPermissionResponse(permission, currentUserId, documentId, userInfo);
    }

    /**
     * Remove a user's access to a document
     */
    @Transactional
    public void removePermission(
            String documentId,
            UUID targetUserId,
            UUID currentUserId,
            HttpServletRequest httpRequest) {

        log.info("Removing permission for user {} on document {} by user {}",
                targetUserId, documentId, currentUserId);

        // Verify current user can manage permissions
        validateCanManagePermissions(documentId, currentUserId);

        // Get target permission
        DocumentPermission permission = permissionRepository
                .findByDocumentIdAndUserId(documentId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Permission not found for user " + targetUserId));

        // Don't allow removing owner
        if (permission.getPermissionLevel() == PermissionLevel.OWNER) {
            throw new IllegalArgumentException("Cannot remove owner permission");
        }

        // Don't allow editor to remove another editor (only owner can)
        PermissionLevel currentUserLevel = getPermissionLevel(documentId, currentUserId);
        if (currentUserLevel == PermissionLevel.EDITOR &&
                permission.getPermissionLevel() == PermissionLevel.EDITOR) {
            throw new AccessDeniedException("Editors cannot remove other editors");
        }

        permissionRepository.deleteByDocumentIdAndUserId(documentId, targetUserId);

        // Audit log
        auditService.logPermissionRemove(documentId, currentUserId, targetUserId,
                permission.getPermissionLevel(), httpRequest);

        log.info("Permission removed successfully for user {} on document {}",
                targetUserId, documentId);
    }

    /**
     * Get document access information for current user
     */
    @Transactional(readOnly = true)
    public DocumentAccessInfoResponse getDocumentAccessInfo(
            String documentId,
            UUID currentUserId) {
        return getDocumentAccessInfo(documentId, currentUserId, null);
    }


    public DocumentAccessInfoResponse getDocumentAccessInfo(
            String documentId,
            UUID currentUserId,
            HttpServletRequest httpRequest) {

        // Get document info
        DocumentInfo documentInfo = editorServiceClient.getDocumentInfo(documentId);

        // Get user's permission
        PermissionLevel permissionLevel = getPermissionLevel(documentId, currentUserId);

        // Get owner info
        User ownerInfo = authServiceClient.getUserById(documentInfo.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found: " + documentInfo.getOwnerId()));

        return DocumentAccessInfoResponse.builder()
                .documentId(documentId)
                .title(documentInfo.getTitle())
                .userPermission(permissionLevel)
                .canEdit(permissionLevel != null && permissionLevel.canEdit())
                .canShare(permissionLevel != null && permissionLevel.canShare())
                .canManagePermissions(permissionLevel != null && permissionLevel.canManagePermissions())
                .canRequestAccess(permissionLevel == null && documentInfo.isAllowAccessRequests())
                .ownerId(UUID.fromString(documentInfo.getOwnerId()))
                .ownerEmail(ownerInfo.getEmail())
                .ownerName(ownerInfo.getFullName())
                .build();
    }

    // ========================================
    // Helper Methods
    // ========================================

    public PermissionLevel getPermissionLevel(String documentId, UUID userId) {
        return permissionRepository
                .findByDocumentIdAndUserId(documentId, userId)
                .map(DocumentPermission::getPermissionLevel)
                .orElse(null);
    }

    public boolean hasAccess(String documentId, UUID userId, PermissionLevel requiredLevel) {
        PermissionLevel userLevel = getPermissionLevel(documentId, userId);
        return userLevel != null && userLevel.canPerform(requiredLevel);
    }

    private void validateHasAccess(String documentId, UUID userId, PermissionLevel requiredLevel) {
        if (!hasAccess(documentId, userId, requiredLevel)) {
            throw new AccessDeniedException("You don't have access to this document");
        }
    }

    private void validateCanShare(String documentId, UUID userId) {
        PermissionLevel level = getPermissionLevel(documentId, userId);
        if (level == null || !level.canShare()) {
            throw new AccessDeniedException("You don't have permission to share this document");
        }
    }

    private void validateCanManagePermissions(String documentId, UUID userId) {
        PermissionLevel level = getPermissionLevel(documentId, userId);
        if (level == null || !level.canManagePermissions()) {
            throw new AccessDeniedException(
                    "You don't have permission to manage document permissions");
        }
    }

    private DocumentPermissionResponse mapToPermissionResponse(
            DocumentPermission permission,
            UUID currentUserId,
            String documentId,
            User userInfo) {

        PermissionLevel currentUserLevel = getPermissionLevel(documentId, currentUserId);

        boolean canRemove = currentUserLevel != null &&
                currentUserLevel.canManagePermissions() &&
                permission.getPermissionLevel() != PermissionLevel.OWNER &&
                !permission.getUserId().equals(currentUserId);

        boolean canChange = canRemove;

        DocumentPermissionResponse.Builder builder = DocumentPermissionResponse.builder()
                .id(permission.getId())
                .userId(permission.getUserId())
                .permissionLevel(permission.getPermissionLevel())
                .grantedAt(permission.getGrantedAt())
                .canRemove(canRemove)
                .canChangePermission(canChange);

        if (userInfo != null) {
            builder
                    .userEmail(userInfo.getEmail())
                    .userName(userInfo.getFullName())
                    .userAvatarUrl(userInfo.getAvatarUrl());
        }

        return builder.build();
    }

    private ShareResult createFailureResult(String email, String message) {
        return ShareResult.builder()
                .email(email)
                .success(false)
                .message(message)
                .build();
    }
}

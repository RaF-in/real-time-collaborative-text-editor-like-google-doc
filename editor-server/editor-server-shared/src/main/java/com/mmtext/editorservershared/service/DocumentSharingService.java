package com.mmtext.editorservershared.service;

import com.mmtext.editorservershared.dto.*;
import com.mmtext.editorservershared.enums.PermissionLevel;
import com.mmtext.editorservershared.model.DocumentPermission;
import com.mmtext.editorservershared.repo.DocumentPermissionRepository;
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

    public DocumentSharingService(DocumentPermissionRepository permissionRepository, EmailService emailService, AuditService auditService) {
        this.permissionRepository = permissionRepository;
        this.emailService = emailService;
        this.auditService = auditService;
    }


    /**
     * Share document with multiple people at once
     */
    @Transactional
    public ShareMultipleResponse shareWithMultiple(
            UUID documentId,
            ShareWithMultipleRequest request,
            UUID currentUserId,
            HttpServletRequest httpRequest) {

        log.info("Sharing document {} with {} recipients by user {}",
                documentId, request.getRecipients().size(), currentUserId);

        // Verify current user can share
        validateCanShare(documentId, currentUserId);

        // Get document info
        var documentInfo = editorServiceClient.getDocumentInfo(documentId);

        List<ShareResult> results = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        for (var recipient : request.getRecipients()) {
            try {
                // Validate user exists
                var targetUser = authServiceClient.getUserByEmail(recipient.getEmail());

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
                    PermissionLevel oldLevel = permission.getPermissionLevel();
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

                // Send notifications if requested
                if (Boolean.TRUE.equals(request.getNotifyPeople())) {
                    notificationService.sendDocumentSharedNotification(
                            targetUser.getId(),
                            documentId,
                            documentInfo.getTitle(),
                            recipient.getPermissionLevel(),
                            currentUserId,
                            request.getMessage()
                    );

                    emailService.sendDocumentSharedEmail(
                            targetUser.getEmail(),
                            targetUser.getName(),
                            documentId,
                            documentInfo.getTitle(),
                            recipient.getPermissionLevel(),
                            currentUserId,
                            request.getMessage()
                    );
                }

                // Build response
                var permissionResponse = mapToPermissionResponse(permission, currentUserId, documentId);
                permissionResponse.setUserEmail(targetUser.getEmail());
                permissionResponse.setUserName(targetUser.getName());
                permissionResponse.setUserAvatarUrl(targetUser.getAvatarUrl());

                results.add(ShareMultipleResponse.ShareResult.builder()
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
            UUID documentId,
            UUID currentUserId) {

        // Verify user has access
        validateHasAccess(documentId, currentUserId, PermissionLevel.VIEWER);

        List<DocumentPermission> permissions = permissionRepository.findByDocumentId(documentId);

        // Enrich with user information
        return permissions.stream()
                .map(permission -> {
                    var response = mapToPermissionResponse(permission, currentUserId, documentId);

                    // Fetch user info
                    try {
                        var userInfo = authServiceClient.getUserById(permission.getUserId());
                        response.setUserEmail(userInfo.getEmail());
                        response.setUserName(userInfo.getName());
                        response.setUserAvatarUrl(userInfo.getAvatarUrl());
                    } catch (Exception e) {
                        log.warn("Failed to fetch user info for {}: {}", permission.getUserId(), e.getMessage());
                    }

                    return response;
                })
                .collect(Collectors.toList());
    }

    /**
     * Update a user's permission level
     */
    @Transactional
    public DocumentPermissionResponse updatePermission(
            UUID documentId,
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

        // Notify user
        var documentInfo = editorServiceClient.getDocumentInfo(documentId);
        notificationService.sendPermissionChangedNotification(
                request.getUserId(),
                documentId,
                documentInfo.getTitle(),
                request.getPermissionLevel()
        );

        log.info("Permission updated successfully for user {} on document {}",
                request.getUserId(), documentId);

        return mapToPermissionResponse(permission, currentUserId, documentId);
    }

    /**
     * Remove a user's access to a document
     */
    @Transactional
    public void removePermission(
            UUID documentId,
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

        // Notify user
        var documentInfo = editorServiceClient.getDocumentInfo(documentId);
        notificationService.sendPermissionRemovedNotification(
                targetUserId,
                documentId,
                documentInfo.getTitle()
        );

        log.info("Permission removed successfully for user {} on document {}",
                targetUserId, documentId);
    }

    /**
     * Get document access information for current user
     */
    @Transactional(readOnly = true)
    public DocumentAccessInfoResponse getDocumentAccessInfo(
            UUID documentId,
            UUID currentUserId) {

        // Get document info
        var documentInfo = editorServiceClient.getDocumentInfo(documentId);

        // Get user's permission
        PermissionLevel permissionLevel = getPermissionLevel(documentId, currentUserId);

        // Get owner info
        var ownerInfo = authServiceClient.getUserById(documentInfo.getOwnerId());

        return DocumentAccessInfoResponse.builder()
                .documentId(documentId)
                .title(documentInfo.getTitle())
                .userPermission(permissionLevel)
                .canEdit(permissionLevel != null && permissionLevel.canEdit())
                .canShare(permissionLevel != null && permissionLevel.canShare())
                .canManagePermissions(permissionLevel != null && permissionLevel.canManagePermissions())
                .canRequestAccess(permissionLevel == null && documentInfo.getAllowAccessRequests())
                .ownerId(documentInfo.getOwnerId())
                .ownerEmail(ownerInfo.getEmail())
                .ownerName(ownerInfo.getName())
                .build();
    }

    // ========================================
    // Helper Methods
    // ========================================

    public PermissionLevel getPermissionLevel(UUID documentId, UUID userId) {
        return permissionRepository
                .findByDocumentIdAndUserId(documentId, userId)
                .map(DocumentPermission::getPermissionLevel)
                .orElse(null);
    }

    public boolean hasAccess(UUID documentId, UUID userId, PermissionLevel requiredLevel) {
        PermissionLevel userLevel = getPermissionLevel(documentId, userId);
        return userLevel != null && userLevel.canPerform(requiredLevel);
    }

    private void validateHasAccess(UUID documentId, UUID userId, PermissionLevel requiredLevel) {
        if (!hasAccess(documentId, userId, requiredLevel)) {
            throw new AccessDeniedException("You don't have access to this document");
        }
    }

    private void validateCanShare(UUID documentId, UUID userId) {
        PermissionLevel level = getPermissionLevel(documentId, userId);
        if (level == null || !level.canShare()) {
            throw new AccessDeniedException("You don't have permission to share this document");
        }
    }

    private void validateCanManagePermissions(UUID documentId, UUID userId) {
        PermissionLevel level = getPermissionLevel(documentId, userId);
        if (level == null || !level.canManagePermissions()) {
            throw new AccessDeniedException(
                    "You don't have permission to manage document permissions");
        }
    }

    private DocumentPermissionResponse mapToPermissionResponse(
            DocumentPermission permission,
            UUID currentUserId,
            UUID documentId) {

        PermissionLevel currentUserLevel = getPermissionLevel(documentId, currentUserId);

        boolean canRemove = currentUserLevel != null &&
                currentUserLevel.canManagePermissions() &&
                permission.getPermissionLevel() != PermissionLevel.OWNER &&
                !permission.getUserId().equals(currentUserId);

        boolean canChange = canRemove;

        return DocumentPermissionResponse.builder()
                .id(permission.getId())
                .userId(permission.getUserId())
                .permissionLevel(permission.getPermissionLevel())
                .grantedAt(permission.getGrantedAt())
                .canRemove(canRemove)
                .canChangePermission(canChange)
                .build();
    }

    private ShareMultipleResponse.ShareResult createFailureResult(String email, String message) {
        return ShareMultipleResponse.ShareResult.builder()
                .email(email)
                .success(false)
                .message(message)
                .build();
    }
}
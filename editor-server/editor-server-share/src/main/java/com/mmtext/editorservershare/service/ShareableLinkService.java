package com.mmtext.editorservershare.service;

import com.mmtext.editorservershare.client.grpc.EditorServiceClient;
import com.mmtext.editorservershare.dto.CreateShareableLinkDto;
import com.mmtext.editorservershare.dto.DocumentAccessInfoResponse;
import com.mmtext.editorservershare.dto.ShareableLinkResponse;
import com.mmtext.editorservershare.enums.PermissionLevel;
import com.mmtext.editorservershare.exception.AccessDeniedException;
import com.mmtext.editorservershare.exception.ResourceNotFoundException;
import com.mmtext.editorservershare.model.DocumentPermission;
import com.mmtext.editorservershare.model.ShareableLink;
import com.mmtext.editorservershare.repo.DocumentPermissionRepository;
import com.mmtext.editorservershare.repo.ShareableLinkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Service

public class ShareableLinkService {

    private static final Logger log = LoggerFactory.getLogger(ShareableLinkService.class);
    private final ShareableLinkRepository linkRepository;
    private final DocumentPermissionRepository permissionRepository;
    private final DocumentSharingService sharingService;
    private final EditorServiceClient editorServiceClient;

    private static final String LINK_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int LINK_TOKEN_LENGTH = 32;
    private final SecureRandom secureRandom = new SecureRandom();

    public ShareableLinkService(ShareableLinkRepository linkRepository, DocumentPermissionRepository permissionRepository, DocumentSharingService sharingService, EditorServiceClient editorServiceClient) {
        this.linkRepository = linkRepository;
        this.permissionRepository = permissionRepository;
        this.sharingService = sharingService;
        this.editorServiceClient = editorServiceClient;
    }

    /**
     * Create a shareable link
     */
    @Transactional
    public ShareableLinkResponse createShareableLink(
            String documentId,
            CreateShareableLinkDto request,
            UUID currentUserId,
            String baseUrl) {
        log.info("Creating shareable link for document {} by user {}", documentId, currentUserId);

        // Verify user can share
        if (!sharingService.hasAccess(documentId, currentUserId,
                PermissionLevel.VIEWER)) {
            throw new AccessDeniedException("You don't have permission to create shareable links");
        }

        // Additional check - user must have sharing permission
        PermissionLevel userPermission = sharingService.getPermissionLevel(documentId, currentUserId);
        if (userPermission == null || !userPermission.canShare()) {
            throw new AccessDeniedException("You don't have permission to create shareable links");
        }

        // Validate permission level
        if (request.getPermissionLevel() == PermissionLevel.OWNER) {
            throw new IllegalArgumentException("Cannot create link with owner permission");
        }

        // Generate unique token
        String token = generateUniqueToken();

        ShareableLink link = ShareableLink.builder()
                .documentId(documentId)
                .linkToken(token)
                .permissionLevel(request.getPermissionLevel())
                .createdBy(currentUserId)
                .build();

        if (request.getExpiresInDays() != null && request.getExpiresInDays() > 0) {
            link.setExpiresAt(Instant.now().plusSeconds(request.getExpiresInDays() * 86400L));
        }

        link = linkRepository.save(link);

        log.info("Shareable link created: {}", link.getId());

        return mapToResponse(link, baseUrl);
    }

    /**
     * Get active shareable links for a document
     */
    @Transactional(readOnly = true)
    public List<ShareableLinkResponse> getActiveShareableLinks(
            String documentId,
            UUID currentUserId,
            String baseUrl) {

        // Verify user can view links
        if (!sharingService.hasAccess(documentId, currentUserId,
                PermissionLevel.VIEWER)) {
            throw new AccessDeniedException("You don't have permission to view shareable links");
        }

        List<ShareableLink> links = linkRepository.findActiveByDocumentId(documentId, Instant.now());

        return links.stream()
                .map(link -> mapToResponse(link, baseUrl))
                .collect(Collectors.toList());
    }

    /**
     * Deactivate a shareable link
     */
    @Transactional
    public void deactivateShareableLink(UUID linkId, UUID currentUserId) {

        ShareableLink link = linkRepository.findById(linkId)
                .orElseThrow(() -> new ResourceNotFoundException("Link not found"));

        // Verify user can manage links
        if (!sharingService.hasAccess(link.getDocumentId(), currentUserId,
                PermissionLevel.VIEWER)) {
            throw new AccessDeniedException("You don't have permission to revoke this link");
        }

        // Additional check - user must have sharing permission or be the creator
        PermissionLevel userPermission = sharingService.getPermissionLevel(link.getDocumentId(), currentUserId);
        if ((userPermission == null || !userPermission.canShare()) &&
            !link.getCreatedBy().equals(currentUserId)) {
            throw new AccessDeniedException("You don't have permission to revoke this link");
        }

        link.deactivate();
        linkRepository.save(link);

        log.info("Shareable link {} deactivated", linkId);
    }

    /**
     * Access via shareable link and return document ID for redirect
     */
    @Transactional
    public String accessViaShareableLinkAndGetDocumentId(String token, UUID userId) {

        ShareableLink link = linkRepository.findByLinkToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired link"));

        if (!link.isValid()) {
            throw new IllegalArgumentException("This link has expired or been deactivated");
        }

        // Update link usage
        link.incrementAccessCount();
        linkRepository.save(link);

        // Grant permission if user is authenticated and doesn't have access
        if (userId != null) {
            boolean hasAccess = sharingService.hasAccess(link.getDocumentId(), userId,
                    PermissionLevel.VIEWER);

            if (!hasAccess) {
                // Auto-grant permission via link
                DocumentPermission permission = DocumentPermission.builder()
                        .documentId(link.getDocumentId())
                        .userId(userId)
                        .permissionLevel(link.getPermissionLevel())
                        .grantedBy(link.getCreatedBy())
                        .build();

                permissionRepository.save(permission);
                log.info("Auto-granted {} permission to user {} via link",
                        link.getPermissionLevel(), userId);
            }
        }

        return link.getDocumentId();
    }

    /**
     * Access via shareable link and return full document info
     */
    @Transactional
    public DocumentAccessInfoResponse accessViaShareableLink(String token, UUID userId) {
        // Get the shareable link to retrieve document ID
        ShareableLink link = linkRepository.findByLinkToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired link"));

        if (!link.isValid()) {
            throw new IllegalArgumentException("This link has expired or been deactivated");
        }

        // Grant access if user doesn't already have it
        if (userId != null) {
            boolean hasAccess = sharingService.hasAccess(link.getDocumentId(), userId,
                    PermissionLevel.VIEWER);

            if (!hasAccess) {
                // Auto-grant permission via link
                DocumentPermission permission = DocumentPermission.builder()
                        .documentId(link.getDocumentId())
                        .userId(userId)
                        .permissionLevel(link.getPermissionLevel())
                        .grantedBy(link.getCreatedBy())
                        .build();

                permissionRepository.save(permission);
                log.info("Auto-granted {} permission to user {} via link",
                        link.getPermissionLevel(), userId);
            }
        }

        // Get document access info using the document ID
        return sharingService.getDocumentAccessInfo(link.getDocumentId(), userId, null);
    }

    // ========================================
    // Helper Methods
    // ========================================

    private String generateUniqueToken() {
        StringBuilder token = new StringBuilder(LINK_TOKEN_LENGTH);
        for (int i = 0; i < LINK_TOKEN_LENGTH; i++) {
            token.append(LINK_CHARS.charAt(secureRandom.nextInt(LINK_CHARS.length())));
        }

        // Ensure uniqueness
        if (linkRepository.existsByLinkToken(token.toString())) {
            return generateUniqueToken();
        }

        return token.toString();
    }

    private ShareableLinkResponse mapToResponse(ShareableLink link, String baseUrl) {
        return ShareableLinkResponse.builder()
                .id(link.getId())
                .linkToken(link.getLinkToken())
                .fullUrl(baseUrl + "/api/share/link/" + link.getLinkToken())
                .permissionLevel(link.getPermissionLevel())
                .createdAt(link.getCreatedAt())
                .expiresAt(link.getExpiresAt())
                .isActive(link.getActive())
                .accessCount(link.getAccessCount())
                .lastAccessedAt(link.getLastAccessedAt())
                .build();
    }
}
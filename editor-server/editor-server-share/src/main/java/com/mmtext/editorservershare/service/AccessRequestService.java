package com.mmtext.editorservershare.service;

import com.mmtext.editorservershare.client.grpc.AuthServiceClient;
import com.mmtext.editorservershare.client.grpc.EditorServiceClient;
import com.mmtext.editorservershare.domain.User;
import com.mmtext.editorservershare.domain.Document;
import com.mmtext.editorservershare.domain.DocumentInfo;
import com.mmtext.editorservershare.dto.*;
import com.mmtext.editorservershare.enums.RequestStatus;
import com.mmtext.editorservershare.exception.ResourceNotFoundException;
import com.mmtext.editorservershare.enums.PermissionLevel;
import org.springframework.security.access.AccessDeniedException;
import com.mmtext.editorservershare.model.AccessRequest;
import com.mmtext.editorservershare.model.DocumentPermission;
import com.mmtext.editorservershare.repo.AccessRequestRepository;
import com.mmtext.editorservershare.repo.DocumentPermissionRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AccessRequestService {

    private static final Logger log = LoggerFactory.getLogger(AccessRequestService.class);

    private final AccessRequestRepository accessRequestRepository;
    private final DocumentPermissionRepository permissionRepository;
    private final EditorServiceClient editorServiceClient;
    private final AuthServiceClient authServiceClient;
    private final EmailService emailService;
    private final AuditService auditService;

    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public AccessRequestService(
            AccessRequestRepository accessRequestRepository,
            DocumentPermissionRepository permissionRepository,
            EditorServiceClient editorServiceClient,
            AuthServiceClient authServiceClient,
            EmailService emailService,
            AuditService auditService) {
        this.accessRequestRepository = accessRequestRepository;
        this.permissionRepository = permissionRepository;
        this.editorServiceClient = editorServiceClient;
        this.authServiceClient = authServiceClient;
        this.emailService = emailService;
        this.auditService = auditService;
    }

    @Transactional
    public AccessRequestResponse requestAccess(
            String documentId,
            RequestAccessDto request,
            UUID currentUserId,
            HttpServletRequest httpRequest) {
        return requestAccess(documentId, null, request, currentUserId, httpRequest);
    }

    @Transactional
    public AccessRequestResponse requestAccess(
            String documentId,
            String originalDocumentId,
            RequestAccessDto request,
            UUID currentUserId,
            HttpServletRequest httpRequest) {

        // Check existing access
        if (permissionRepository.existsByDocumentIdAndUserId(documentId, currentUserId)) {
            throw new IllegalArgumentException("You already have access to this document");
        }

        DocumentInfo documentInfo = editorServiceClient.getDocumentInfo(
            originalDocumentId != null ? originalDocumentId : documentId.toString()
        );
        if (!documentInfo.isAllowAccessRequests()) {
            throw new IllegalArgumentException("This document doesn't accept access requests");
        }

        if (accessRequestRepository.existsByDocumentIdAndRequesterIdAndStatus(
                documentId, currentUserId, RequestStatus.PENDING)) {
            throw new IllegalArgumentException("You already have a pending request");
        }

        var requesterInfo = authServiceClient.getUserById(currentUserId.toString())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUserId));

        AccessRequest accessRequest = AccessRequest.builder()
                .documentId(documentId)
                .requesterId(currentUserId)
                .requesterEmail(requesterInfo.getEmail())
                .requesterName(requesterInfo.getFullName())
                .requestedPermission(request.getRequestedPermission())
                .message(request.getMessage())
                .status(RequestStatus.PENDING)
                .build();

        accessRequest = accessRequestRepository.save(accessRequest);

        // Generate approval token and set expiry (24 hours)
        String token = generateToken();
        Instant tokenExpiresAt = Instant.now().plusSeconds(24 * 60 * 60);
        log.info("Generated token {} for request ID {} with expiry {}", token, accessRequest.getId(), tokenExpiresAt);

        // Save token to database
        accessRequest.setApprovalToken(token);
        accessRequest.setTokenExpiresAt(tokenExpiresAt);
        accessRequest = accessRequestRepository.save(accessRequest);
        log.info("Saved token to database for request ID {}", accessRequest.getId());

        // Send email to owner
        var ownerInfo = authServiceClient.getUserById(documentInfo.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found: " + documentInfo.getOwnerId()));
        emailService.sendAccessRequestEmail(
                ownerInfo.getEmail(),
                ownerInfo.getFullName(),
                documentId,
                documentInfo.getTitle(),
                requesterInfo.getFullName(),
                requesterInfo.getEmail(),
                request.getRequestedPermission(),
                request.getMessage(),
                accessRequest.getId(),
                token
        );

        auditService.logAccessRequest(documentId, currentUserId,
                request.getRequestedPermission(), httpRequest);

        return mapToResponse(accessRequest, documentInfo);
    }

    @Transactional
    public void approveViaEmail(UUID requestId, String token) {
        log.info("Approving access request {} with token {}", requestId, token);
        validateToken(requestId, token);

        AccessRequest request = accessRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalArgumentException("Request already resolved");
        }

        request.approve(request.getDocumentId()); // Use doc owner as resolver

        // Grant permission
        DocumentPermission permission = DocumentPermission.builder()
                .documentId(request.getDocumentId())
                .userId(request.getRequesterId())
                .permissionLevel(request.getRequestedPermission())
                .grantedBy(null) // Via email
                .build();

        log.info("Creating permission for document {} and user {}",
                request.getDocumentId(), request.getRequesterId());
        permission = permissionRepository.save(permission);
        log.info("Permission created with ID {}", permission.getId());

        request = accessRequestRepository.save(request);
        log.info("Access request {} marked as approved", requestId);

        // Get document info
        String originalDocId = request.getDocumentId();
        if (originalDocId == null) {
            // If original document ID is not stored, get it from document info
            var documentInfo = editorServiceClient.getDocumentInfo(request.getDocumentId().toString());
            originalDocId = documentInfo.getDocId();

            // Send approval email
            emailService.sendAccessApprovedEmail(
                    request.getRequesterEmail(),
                    request.getRequesterName(),
                    originalDocId,
                    documentInfo.getTitle(),
                    request.getRequestedPermission()
            );
        } else {
            // Original document ID is available, get title separately
            var documentInfo = editorServiceClient.getDocumentInfo(originalDocId);
            emailService.sendAccessApprovedEmail(
                    request.getRequesterEmail(),
                    request.getRequesterName(),
                    originalDocId,
                    documentInfo.getTitle(),
                    request.getRequestedPermission()
            );
        }

        // Clear the token after use
        request.setApprovalToken(null);
        accessRequestRepository.save(request);
        log.info("Access request {} approved via email", requestId);
    }

    @Transactional
    public void rejectViaEmail(UUID requestId, String token) {
        validateToken(requestId, token);

        AccessRequest request = accessRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        request.reject(request.getDocumentId());
        accessRequestRepository.save(request);

        // Send rejection email
        var documentInfo = editorServiceClient.getDocumentInfo(request.getDocumentId().toString());
        emailService.sendAccessRejectedEmail(
                request.getRequesterEmail(),
                request.getRequesterName(),
                documentInfo.getTitle()
        );

        // Clear the token after use
        request.setApprovalToken(null);
        accessRequestRepository.save(request);
        log.info("Access request {} rejected via email", requestId);
    }

    public List<AccessRequestResponse> getPendingAccessRequests(String documentId, UUID currentUserId) {
        // Verify owner
        if (!permissionRepository.existsByDocumentIdAndUserIdAndPermissionLevel(
                documentId, currentUserId, PermissionLevel.OWNER)) {
            throw new AccessDeniedException("Only owner can view access requests");
        }

        DocumentInfo documentInfo = editorServiceClient.getDocumentInfo(documentId.toString());
        return accessRequestRepository.findByDocumentIdAndStatus(documentId, RequestStatus.PENDING)
                .stream()
                .map(req -> mapToResponse(req, documentInfo))
                .toList();
    }

    private void validateToken(UUID requestId, String token) {
        log.info("Validating token for request ID: {}", requestId);

        AccessRequest request = accessRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found: " + requestId));

        String storedToken = request.getApprovalToken();
        Instant expiresAt = request.getTokenExpiresAt();

        if (storedToken == null) {
            log.error("No token found for request ID: {}", requestId);
            throw new IllegalArgumentException("Invalid or expired token");
        }

        if (expiresAt != null && expiresAt.isBefore(Instant.now())) {
            log.error("Token expired for request ID: {}. Expired at: {}", requestId, expiresAt);
            throw new IllegalArgumentException("Token has expired");
        }

        if (!storedToken.equals(token)) {
            log.error("Token mismatch for request ID: {}. Expected: {}, Got: {}", requestId, storedToken, token);
            throw new IllegalArgumentException("Invalid or expired token");
        }

        log.info("Token validated successfully for request ID: {}", requestId);
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private AccessRequestResponse mapToResponse(AccessRequest request,
                                                DocumentInfo documentInfo) {
        return AccessRequestResponse.builder()
                .id(request.getId())
                .documentId(request.getDocumentId())
                .documentTitle(documentInfo.getTitle())
                .requesterId(request.getRequesterId())
                .requesterEmail(request.getRequesterEmail())
                .requesterName(request.getRequesterName())
                .requestedPermission(request.getRequestedPermission())
                .message(request.getMessage())
                .status(request.getStatus())
                .requestedAt(request.getRequestedAt())
                .build();
    }
}
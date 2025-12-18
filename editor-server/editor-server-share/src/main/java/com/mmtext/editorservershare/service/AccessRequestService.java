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

    private final Map<String, String> approvalTokens = new HashMap<>();
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
            UUID documentId,
            RequestAccessDto request,
            UUID currentUserId,
            HttpServletRequest httpRequest) {

        // Check existing access
        if (permissionRepository.existsByDocumentIdAndUserId(documentId, currentUserId)) {
            throw new IllegalArgumentException("You already have access to this document");
        }

        DocumentInfo documentInfo = editorServiceClient.getDocumentInfo(documentId.toString());
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

        // Generate approval token
        String token = generateToken();
        approvalTokens.put(accessRequest.getId().toString(), token);

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

        permissionRepository.save(permission);
        accessRequestRepository.save(request);

        // Send approval email
        var documentInfo = editorServiceClient.getDocumentInfo(request.getDocumentId().toString());
        emailService.sendAccessApprovedEmail(
                request.getRequesterEmail(),
                request.getRequesterName(),
                request.getDocumentId(),
                documentInfo.getTitle(),
                request.getRequestedPermission()
        );

        approvalTokens.remove(requestId.toString());
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

        approvalTokens.remove(requestId.toString());
        log.info("Access request {} rejected via email", requestId);
    }

    public List<AccessRequestResponse> getPendingAccessRequests(UUID documentId, UUID currentUserId) {
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
        String storedToken = approvalTokens.get(requestId);
        if (storedToken == null || !storedToken.equals(token)) {
            throw new IllegalArgumentException("Invalid or expired token");
        }
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
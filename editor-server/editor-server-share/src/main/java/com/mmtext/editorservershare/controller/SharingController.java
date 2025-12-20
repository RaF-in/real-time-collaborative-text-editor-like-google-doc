package com.mmtext.editorservershare.controller;

import com.mmtext.editorservershare.annotation.CurrentUser;
import com.mmtext.editorservershare.dto.*;
import com.mmtext.editorservershare.service.AccessRequestService;
import com.mmtext.editorservershare.service.DocumentSharingService;
import com.mmtext.editorservershare.service.ShareableLinkService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/share")
public class SharingController {

    private final DocumentSharingService sharingService;
    private final AccessRequestService accessRequestService;
    private final ShareableLinkService linkService;

    public SharingController(DocumentSharingService sharingService, AccessRequestService accessRequestService, ShareableLinkService linkService) {
        this.sharingService = sharingService;
        this.accessRequestService = accessRequestService;
        this.linkService = linkService;
    }

    /**
     * Convert string document ID to UUID using a consistent approach
     * This ensures that the same string document ID always maps to the same UUID
     */
    private UUID convertToUuid(String documentId) {
        // Use a consistent namespace UUID for deterministic conversion
        UUID namespace = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        return UUID.nameUUIDFromBytes((namespace + documentId).getBytes());
    }

    // ========================================
    // Document Sharing
    // ========================================

    @PostMapping("/documents/{documentId}/share-multiple")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ShareMultipleResponse> shareWithMultiple(
            @PathVariable String documentId,
            @Valid @RequestBody ShareWithMultipleRequest request,
            @CurrentUser UUID currentUserId,
            HttpServletRequest httpRequest) {

        ShareMultipleResponse response = sharingService.shareWithMultiple(
                convertToUuid(documentId), documentId, request, currentUserId, httpRequest
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/documents/{documentId}/permissions")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<DocumentPermissionResponse>> getPermissions(
            @PathVariable String documentId,
            @CurrentUser UUID currentUserId) {

        return ResponseEntity.ok(sharingService.getDocumentPermissions(convertToUuid(documentId), currentUserId));
    }

    @PutMapping("/documents/{documentId}/permissions")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<DocumentPermissionResponse> updatePermission(
            @PathVariable String documentId,
            @Valid @RequestBody UpdatePermissionDto request,
            @CurrentUser UUID currentUserId,
            HttpServletRequest httpRequest) {

        return ResponseEntity.ok(sharingService.updatePermission(convertToUuid(documentId), request, currentUserId, httpRequest));
    }

    @DeleteMapping("/documents/{documentId}/permissions/{userId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> removePermission(
            @PathVariable String documentId,
            @PathVariable UUID userId,
            @CurrentUser UUID currentUserId,
            HttpServletRequest httpRequest) {

        sharingService.removePermission(convertToUuid(documentId), userId, currentUserId, httpRequest);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/documents/{documentId}/access-info")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<DocumentAccessInfoResponse> getAccessInfo(
            @PathVariable String documentId,
            @CurrentUser UUID currentUserId,
            HttpServletRequest httpRequest) {

        return ResponseEntity.ok(sharingService.getDocumentAccessInfo(convertToUuid(documentId), documentId, currentUserId, httpRequest));
    }

    // ========================================
    // Access Requests
    // ========================================

    @PostMapping("/documents/{documentId}/request-access")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<AccessRequestResponse> requestAccess(
            @PathVariable String documentId,
            @Valid @RequestBody RequestAccessDto request,
            @CurrentUser UUID currentUserId,
            HttpServletRequest httpRequest) {

        return ResponseEntity.ok(accessRequestService.requestAccess(convertToUuid(documentId), request, currentUserId, httpRequest));
    }

    @GetMapping("/documents/{documentId}/access-requests")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<AccessRequestResponse>> getPendingRequests(
            @PathVariable String documentId,
            @CurrentUser UUID currentUserId) {

        return ResponseEntity.ok(accessRequestService.getPendingAccessRequests(convertToUuid(documentId), currentUserId));
    }

    // Email link handlers - No authentication required
    @GetMapping("/access-requests/{requestId}/approve")
    public RedirectView approveAccessRequest(
            @PathVariable UUID requestId,
            @RequestParam String token) {

        accessRequestService.approveViaEmail(requestId, token);
        return new RedirectView("/documents?access=approved");
    }

    @GetMapping("/access-requests/{requestId}/reject")
    public RedirectView rejectAccessRequest(
            @PathVariable UUID requestId,
            @RequestParam String token) {

        accessRequestService.rejectViaEmail(requestId, token);
        return new RedirectView("/documents?access=rejected");
    }

    // ========================================
    // Shareable Links
    // ========================================

    @PostMapping("/documents/{documentId}/links")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ShareableLinkResponse> createLink(
            @PathVariable String documentId,
            @Valid @RequestBody CreateShareableLinkDto request,
            @CurrentUser UUID currentUserId,
            HttpServletRequest httpRequest) {

        String baseUrl = getBaseUrl(httpRequest);
        return ResponseEntity.ok(linkService.createShareableLink(convertToUuid(documentId), request, currentUserId, baseUrl));
    }

    @GetMapping("/documents/{documentId}/links")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<ShareableLinkResponse>> getLinks(
            @PathVariable String documentId,
            @CurrentUser UUID currentUserId,
            HttpServletRequest httpRequest) {

        String baseUrl = getBaseUrl(httpRequest);
        return ResponseEntity.ok(linkService.getActiveShareableLinks(convertToUuid(documentId), currentUserId, baseUrl));
    }

    @DeleteMapping("/links/{linkId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> revokeLink(
            @PathVariable UUID linkId,
            @CurrentUser UUID currentUserId) {

        linkService.deactivateShareableLink(linkId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    // Public link access - redirects to document
    @GetMapping("/link/{token}")
    public RedirectView accessViaLink(
            @PathVariable String token,
            @CurrentUser(required = false) UUID currentUserId) {

        UUID documentId = linkService.accessViaShareableLinkAndGetDocumentId(token, currentUserId);
        return new RedirectView("/editor/" + documentId);
    }

    private String getBaseUrl(HttpServletRequest request) {
        return request.getScheme() + "://" + request.getServerName() +
                (request.getServerPort() != 80 && request.getServerPort() != 443
                        ? ":" + request.getServerPort() : "");
    }
}

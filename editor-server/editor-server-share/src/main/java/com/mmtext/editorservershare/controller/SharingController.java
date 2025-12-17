package com.mmtext.editorservershare.controller;

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

    // ========================================
    // Document Sharing
    // ========================================

    @PostMapping("/documents/{documentId}/share-multiple")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ShareMultipleResponse> shareWithMultiple(
            @PathVariable UUID documentId,
            @Valid @RequestBody ShareWithMultipleRequest request,
            @CurrentUser UUID currentUserId,
            HttpServletRequest httpRequest) {

        ShareMultipleResponse response = sharingService.shareWithMultiple(
                documentId, request, currentUserId, httpRequest
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/documents/{documentId}/permissions")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<DocumentPermissionResponse>> getPermissions(
            @PathVariable UUID documentId,
            @CurrentUser UUID currentUserId) {

        return ResponseEntity.ok(sharingService.getDocumentPermissions(documentId, currentUserId));
    }

    @PutMapping("/documents/{documentId}/permissions")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<DocumentPermissionResponse> updatePermission(
            @PathVariable UUID documentId,
            @Valid @RequestBody UpdatePermissionDto request,
            @CurrentUser UUID currentUserId,
            HttpServletRequest httpRequest) {

        return ResponseEntity.ok(sharingService.updatePermission(documentId, request, currentUserId, httpRequest));
    }

    @DeleteMapping("/documents/{documentId}/permissions/{userId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> removePermission(
            @PathVariable UUID documentId,
            @PathVariable UUID userId,
            @CurrentUser UUID currentUserId,
            HttpServletRequest httpRequest) {

        sharingService.removePermission(documentId, userId, currentUserId, httpRequest);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/documents/{documentId}/access-info")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<DocumentAccessInfoResponse> getAccessInfo(
            @PathVariable UUID documentId,
            @CurrentUser UUID currentUserId) {

        return ResponseEntity.ok(sharingService.getDocumentAccessInfo(documentId, currentUserId));
    }

    // ========================================
    // Access Requests
    // ========================================

    @PostMapping("/documents/{documentId}/request-access")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<AccessRequestResponse> requestAccess(
            @PathVariable UUID documentId,
            @Valid @RequestBody RequestAccessDto request,
            @CurrentUser UUID currentUserId,
            HttpServletRequest httpRequest) {

        return ResponseEntity.ok(accessRequestService.requestAccess(documentId, request, currentUserId, httpRequest));
    }

    @GetMapping("/documents/{documentId}/access-requests")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<AccessRequestResponse>> getPendingRequests(
            @PathVariable UUID documentId,
            @CurrentUser UUID currentUserId) {

        return ResponseEntity.ok(accessRequestService.getPendingAccessRequests(documentId, currentUserId));
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
            @PathVariable UUID documentId,
            @Valid @RequestBody CreateShareableLinkDto request,
            @CurrentUser UUID currentUserId,
            HttpServletRequest httpRequest) {

        String baseUrl = getBaseUrl(httpRequest);
        return ResponseEntity.ok(linkService.createShareableLink(documentId, request, currentUserId, baseUrl));
    }

    @GetMapping("/documents/{documentId}/links")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<ShareableLinkResponse>> getLinks(
            @PathVariable UUID documentId,
            @CurrentUser UUID currentUserId,
            HttpServletRequest httpRequest) {

        String baseUrl = getBaseUrl(httpRequest);
        return ResponseEntity.ok(linkService.getActiveShareableLinks(documentId, currentUserId, baseUrl));
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

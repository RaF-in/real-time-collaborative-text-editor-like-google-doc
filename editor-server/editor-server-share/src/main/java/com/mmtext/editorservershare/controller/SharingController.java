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
import java.util.Map;
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
            @PathVariable String documentId,
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
            @PathVariable String documentId,
            @CurrentUser UUID currentUserId) {

        return ResponseEntity.ok(sharingService.getDocumentPermissions(documentId, currentUserId));
    }

    @PutMapping("/documents/{documentId}/permissions")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<DocumentPermissionResponse> updatePermission(
            @PathVariable String documentId,
            @Valid @RequestBody UpdatePermissionDto request,
            @CurrentUser UUID currentUserId,
            HttpServletRequest httpRequest) {

        return ResponseEntity.ok(sharingService.updatePermission(documentId, request, currentUserId, httpRequest));
    }

    @DeleteMapping("/documents/{documentId}/permissions/{userId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> removePermission(
            @PathVariable String documentId,
            @PathVariable UUID userId,
            @CurrentUser UUID currentUserId,
            HttpServletRequest httpRequest) {

        sharingService.removePermission(documentId, userId, currentUserId, httpRequest);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/documents/{documentId}/access-info")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<DocumentAccessInfoResponse> getAccessInfo(
            @PathVariable String documentId,
            @CurrentUser UUID currentUserId,
            HttpServletRequest httpRequest) {

        return ResponseEntity.ok(sharingService.getDocumentAccessInfo(documentId, currentUserId, httpRequest));
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

        return ResponseEntity.ok(accessRequestService.requestAccess(documentId, request, currentUserId, httpRequest));
    }

    @GetMapping("/documents/{documentId}/access-requests")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<AccessRequestResponse>> getPendingRequests(
            @PathVariable String documentId,
            @CurrentUser UUID currentUserId) {

        return ResponseEntity.ok(accessRequestService.getPendingAccessRequests(documentId, currentUserId));
    }

    // Email link handlers - No authentication required
    @GetMapping("/access-requests/{requestId}/approve")
    public ResponseEntity<String> approveAccessRequest(
            @PathVariable String requestId,
            @RequestParam(required = false) String token) {

        System.out.println("=== APPROVE ACCESS REQUEST ===");
        System.out.println("RequestId (String): " + requestId);
        System.out.println("Token: " + token);

        if (token == null || token.trim().isEmpty()) {
            System.err.println("Token is null or empty");
            String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Error</title>
                    <style>
                        body { font-family: Arial, sans-serif; text-align: center; padding: 50px; background: #f0f0f0; }
                        .container { background: white; padding: 40px; border-radius: 8px; max-width: 500px; margin: 0 auto; }
                        .error { color: #f44336; font-size: 48px; margin-bottom: 20px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="error">✗</div>
                        <h1>Invalid Link</h1>
                        <p>The approval link is invalid or missing the token.</p>
                        <p>Please contact the requester to send a new request.</p>
                    </div>
                </body>
                </html>
                """;
            return ResponseEntity.badRequest().contentType(org.springframework.http.MediaType.TEXT_HTML).body(html);
        }

        UUID requestUuid;
        try {
            requestUuid = UUID.fromString(requestId);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid UUID format: " + requestId);
            String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Error</title>
                    <style>
                        body { font-family: Arial, sans-serif; text-align: center; padding: 50px; background: #f0f0f0; }
                        .container { background: white; padding: 40px; border-radius: 8px; max-width: 500px; margin: 0 auto; }
                        .error { color: #f44336; font-size: 48px; margin-bottom: 20px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="error">✗</div>
                        <h1>Invalid Request</h1>
                        <p>The request ID is not valid.</p>
                    </div>
                </body>
                </html>
                """;
            return ResponseEntity.badRequest().contentType(org.springframework.http.MediaType.TEXT_HTML).body(html);
        }

        try {
            accessRequestService.approveViaEmail(requestUuid, token);
            String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Access Approved</title>
                    <style>
                        body { font-family: Arial, sans-serif; text-align: center; padding: 50px; background: #f0f0f0; }
                        .container { background: white; padding: 40px; border-radius: 8px; max-width: 500px; margin: 0 auto; }
                        .success { color: #4caf50; font-size: 48px; margin-bottom: 20px; }
                        h1 { color: #333; }
                        p { color: #666; }
                        .close-hint { font-size: 14px; color: #999; margin-top: 30px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="success">✓</div>
                        <h1>Access Approved</h1>
                        <p>The access request has been approved successfully.</p>
                        <p>The user will receive an email notification.</p>
                        <p class="close-hint">You can now close this window.</p>
                    </div>
                </body>
                </html>
                """;
            return ResponseEntity.ok().contentType(org.springframework.http.MediaType.TEXT_HTML).body(html);
        } catch (Exception e) {
            System.err.println("Error approving access request: " + e.getMessage());
            e.printStackTrace();
            String html = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Error</title>
                    <style>
                        body { font-family: Arial, sans-serif; text-align: center; padding: 50px; background: #f0f0f0; }
                        .container { background: white; padding: 40px; border-radius: 8px; max-width: 500px; margin: 0 auto; }
                        .error { color: #f44336; font-size: 48px; margin-bottom: 20px; }
                        h1 { color: #333; }
                        p { color: #666; }
                        .close-hint { font-size: 14px; color: #999; margin-top: 30px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="error">✗</div>
                        <h1>Approval Failed</h1>
                        <p>Unable to approve the access request.</p>
                        <p>Error: %s</p>
                        <p class="close-hint">You can now close this window.</p>
                    </div>
                </body>
                </html>
                """, e.getMessage());
            return ResponseEntity.status(500).contentType(org.springframework.http.MediaType.TEXT_HTML).body(html);
        }
    }

    @GetMapping("/access-requests/{requestId}/reject")
    public ResponseEntity<String> rejectAccessRequest(
            @PathVariable String requestId,
            @RequestParam(required = false) String token) {

        System.out.println("=== REJECT ACCESS REQUEST ===");
        System.out.println("RequestId: " + requestId);
        System.out.println("Token: " + token);

        if (token == null || token.trim().isEmpty()) {
            String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Error</title>
                    <style>
                        body { font-family: Arial, sans-serif; text-align: center; padding: 50px; background: #f0f0f0; }
                        .container { background: white; padding: 40px; border-radius: 8px; max-width: 500px; margin: 0 auto; }
                        .error { color: #f44336; font-size: 48px; margin-bottom: 20px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="error">✗</div>
                        <h1>Invalid Link</h1>
                        <p>The rejection link is invalid or missing the token.</p>
                    </div>
                </body>
                </html>
                """;
            return ResponseEntity.badRequest().contentType(org.springframework.http.MediaType.TEXT_HTML).body(html);
        }

        UUID requestUuid;
        try {
            requestUuid = UUID.fromString(requestId);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid UUID format: " + requestId);
            String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Error</title>
                    <style>
                        body { font-family: Arial, sans-serif; text-align: center; padding: 50px; background: #f0f0f0; }
                        .container { background: white; padding: 40px; border-radius: 8px; max-width: 500px; margin: 0 auto; }
                        .error { color: #f44336; font-size: 48px; margin-bottom: 20px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="error">✗</div>
                        <h1>Invalid Request</h1>
                        <p>The request ID is not valid.</p>
                    </div>
                </body>
                </html>
                """;
            return ResponseEntity.badRequest().contentType(org.springframework.http.MediaType.TEXT_HTML).body(html);
        }

        try {
            accessRequestService.rejectViaEmail(requestUuid, token);
            String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Access Rejected</title>
                    <style>
                        body { font-family: Arial, sans-serif; text-align: center; padding: 50px; background: #f0f0f0; }
                        .container { background: white; padding: 40px; border-radius: 8px; max-width: 500px; margin: 0 auto; }
                        .rejected { color: #f44336; font-size: 48px; margin-bottom: 20px; }
                        h1 { color: #333; }
                        p { color: #666; }
                        .close-hint { font-size: 14px; color: #999; margin-top: 30px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="rejected">✗</div>
                        <h1>Access Rejected</h1>
                        <p>The access request has been rejected.</p>
                        <p>The user will be notified.</p>
                        <p class="close-hint">You can now close this window.</p>
                    </div>
                </body>
                </html>
                """;
            return ResponseEntity.ok().contentType(org.springframework.http.MediaType.TEXT_HTML).body(html);
        } catch (Exception e) {
            String html = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Error</title>
                    <style>
                        body { font-family: Arial, sans-serif; text-align: center; padding: 50px; background: #f0f0f0; }
                        .container { background: white; padding: 40px; border-radius: 8px; max-width: 500px; margin: 0 auto; }
                        .error { color: #f44336; font-size: 48px; margin-bottom: 20px; }
                        h1 { color: #333; }
                        p { color: #666; }
                        .close-hint { font-size: 14px; color: #999; margin-top: 30px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="error">✗</div>
                        <h1>Rejection Failed</h1>
                        <p>Unable to reject the access request.</p>
                        <p>Error: %s</p>
                        <p class="close-hint">You can now close this window.</p>
                    </div>
                </body>
                </html>
                """, e.getMessage());
            return ResponseEntity.badRequest().contentType(org.springframework.http.MediaType.TEXT_HTML).body(html);
        }
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
        return ResponseEntity.ok(linkService.createShareableLink(documentId, request, currentUserId, baseUrl));
    }

    @GetMapping("/documents/{documentId}/links")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<ShareableLinkResponse>> getLinks(
            @PathVariable String documentId,
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

        String documentId = linkService.accessViaShareableLinkAndGetDocumentId(token, currentUserId);
        return new RedirectView("/editor/" + documentId);
    }

    /**
     * Access document via share link and grant permission
     * This endpoint is called after user logs in to grant actual access
     */
    @PostMapping("/access")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<DocumentAccessInfoResponse> accessViaShareLink(
            @RequestBody Map<String, String> request,
            @CurrentUser UUID currentUserId) {

        String token = request.get("token");
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token is required");
        }

        DocumentAccessInfoResponse response = linkService.accessViaShareableLink(token, currentUserId);
        return ResponseEntity.ok(response);
    }

    // Test endpoint
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Share server is working!");
    }

    private String getBaseUrl(HttpServletRequest request) {
        return request.getScheme() + "://" + request.getServerName() +
                (request.getServerPort() != 80 && request.getServerPort() != 443
                        ? ":" + request.getServerPort() : "");
    }
}

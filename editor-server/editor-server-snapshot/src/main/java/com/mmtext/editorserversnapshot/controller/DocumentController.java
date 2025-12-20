package com.mmtext.editorserversnapshot.controller;

import com.mmtext.editorserversnapshot.dto.DocumentStateResponse;
import com.mmtext.editorserversnapshot.model.Document;
import com.mmtext.editorserversnapshot.model.DocumentSnapshot;
import com.mmtext.editorserversnapshot.service.DocumentService;
import com.mmtext.editorserversnapshot.service.SnapshotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API for document operations
 */
@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "*") // Configure based on your requirements
public class DocumentController {

    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);

    private final SnapshotService snapshotService;
    private final DocumentService documentService;

    public DocumentController(SnapshotService snapshotService, DocumentService documentService) {
        this.snapshotService = snapshotService;
        this.documentService = documentService;
    }

    /**
     * Create a new document
     */
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('PERMISSION_DOCUMENT_CREATE')")
    public ResponseEntity<Map<String, Object>> createDocument(Principal principal) {
        logger.info("Creating new document for user: {}", principal.getName());

        String ownerId = extractUserIdFromPrincipal(principal);
        String title = "Untitled Document";

        Document document = documentService.createDocument(title, ownerId);

        // Convert to response format
        Map<String, Object> response = new HashMap<>();
        response.put("id", document.getDocId());
        response.put("title", document.getTitle());
        response.put("createdAt", document.getCreatedAt().toEpochMilli());
        response.put("status", "created");
        response.put("ownerId", document.getOwnerId());

        return ResponseEntity.ok(response);
    }

    /**
     * Check if a document exists (no authentication required for existence check)
     * This allows new document creation without permission validation
     */
    @GetMapping("/{docId}/exists")
    public ResponseEntity<Map<String, Object>> checkDocumentExists(@PathVariable String docId) {
        boolean exists = documentService.documentExists(docId);

        Map<String, Object> response = new HashMap<>();
        response.put("exists", exists);
        response.put("id", docId);

        if (exists) {
            // Get additional document info if it exists
            var documentOpt = documentService.getDocumentByDocId(docId);
            if (documentOpt.isPresent()) {
                Document document = documentOpt.get();
                response.put("title", document.getTitle());
                response.put("createdAt", document.getCreatedAt().toEpochMilli());
            }
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Initialize a new document
     */
    @PostMapping("/{docId}/initialize")
    @PreAuthorize("hasAuthority('PERMISSION_DOCUMENT_CREATE')")
    public ResponseEntity<Map<String, String>> initializeDocument(@PathVariable String docId, Principal principal) {
        logger.info("Initializing document: {}", docId);

        snapshotService.initializeDocument(docId);

        return ResponseEntity.ok(Map.of(
                "docId", docId,
                "status", "initialized",
                "message", "Document initialized successfully"
        ));
    }

    /**
     * Get current document state (snapshot + version vector)
     * Used when a client first connects
     */
    @GetMapping("/{docId}/state")
    @PreAuthorize("hasAuthority('PERMISSION_DOCUMENT_READ')")
    public ResponseEntity<DocumentStateResponse> getDocumentState(@PathVariable String docId, Principal principal) {
        logger.info("Fetching document state: {}", docId);

        List<DocumentSnapshot> snapshot = snapshotService.getDocumentSnapshot(docId);
        Map<String, Long> versionVector = snapshotService.getVersionVector(docId);
        String content = snapshotService.getDocumentContent(docId);

        DocumentStateResponse response = new DocumentStateResponse(
                docId, snapshot, versionVector, content
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Get version vector for a document
     */
    @GetMapping("/{docId}/version")
    @PreAuthorize("hasAuthority('PERMISSION_DOCUMENT_READ')")
    public ResponseEntity<Map<String, Long>> getVersionVector(@PathVariable String docId, Principal principal) {
        logger.info("Fetching version vector: {}", docId);

        Map<String, Long> versionVector = snapshotService.getVersionVector(docId);
        return ResponseEntity.ok(versionVector);
    }

    /**
     * Get document content as plain text
     */
    @GetMapping("/{docId}/content")
    @PreAuthorize("hasAuthority('PERMISSION_DOCUMENT_READ')")
    public ResponseEntity<Map<String, String>> getContent(@PathVariable String docId, Principal principal) {
        logger.info("Fetching document content: {}", docId);

        String content = snapshotService.getDocumentContent(docId);
        return ResponseEntity.ok(Map.of("docId", docId, "content", content));
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "collaborative-editor"
        ));
    }

    /**
     * Extract user ID from principal
     */
    private String extractUserIdFromPrincipal(Principal principal) {
        String username = principal.getName();
        // For now, use username as user ID. In a real implementation,
        // you might need to query user service to get proper user ID
        return username;
    }
}
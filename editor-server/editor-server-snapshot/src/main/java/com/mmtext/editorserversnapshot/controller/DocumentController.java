package com.mmtext.editorserversnapshot.controller;

import com.mmtext.editorserversnapshot.dto.DocumentStateResponse;
import com.mmtext.editorserversnapshot.model.DocumentSnapshot;
import com.mmtext.editorserversnapshot.service.SnapshotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
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

    public DocumentController(SnapshotService snapshotService) {
        this.snapshotService = snapshotService;
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
}
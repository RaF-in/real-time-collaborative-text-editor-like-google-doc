package com.mmtext.editorservermain.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Document Controller for managing document lifecycle
 * 
 * Features:
 * - Create new documents with unique IDs
 * - Check document existence
 * - Support for collaborative editing
 */
@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "*")
public class DocumentController {

    // In-memory storage for document metadata (in production, use a database)
    private final Map<String, DocumentMetadata> documents = new ConcurrentHashMap<>();

    /**
     * Create a new document with a unique ID
     *
     * POST /api/documents/create
     * Response: { "id": "doc-123456789-abc", "createdAt": 1234567890, "title": "Untitled Document" }
     */
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('PERMISSION_DOCUMENT_CREATE')")
    public ResponseEntity<Map<String, Object>> createDocument(Principal principal) {
        // Generate unique document ID
        String timestamp = String.valueOf(System.currentTimeMillis());
        String randomPart = UUID.randomUUID().toString().substring(0, 8);
        String docId = "doc-" + timestamp + "-" + randomPart;
        
        // Create document metadata
        DocumentMetadata metadata = new DocumentMetadata(
            docId,
            "Untitled Document",
            System.currentTimeMillis()
        );
        
        // Store document
        documents.put(docId, metadata);
        
        // Return document info
        Map<String, Object> response = new HashMap<>();
        response.put("id", docId);
        response.put("title", metadata.getTitle());
        response.put("createdAt", metadata.getCreatedAt());
        response.put("status", "created");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Check if a document exists
     *
     * GET /api/documents/{id}/exists
     * Response: { "exists": true, "id": "doc-123" }
     */
    @GetMapping("/{id}/exists")
    @PreAuthorize("hasAuthority('PERMISSION_DOCUMENT_READ')")
    public ResponseEntity<Map<String, Object>> checkDocumentExists(@PathVariable String id, Principal principal) {
        boolean exists = documents.containsKey(id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("exists", exists);
        response.put("id", id);
        
        if (exists) {
            DocumentMetadata metadata = documents.get(id);
            response.put("title", metadata.getTitle());
            response.put("createdAt", metadata.getCreatedAt());
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get document metadata
     *
     * GET /api/documents/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_DOCUMENT_READ')")
    public ResponseEntity<Map<String, Object>> getDocument(@PathVariable String id, Principal principal) {
        DocumentMetadata metadata = documents.get(id);
        
        if (metadata == null) {
            // Document doesn't exist yet - create it on first access
            metadata = new DocumentMetadata(
                id,
                "Untitled Document",
                System.currentTimeMillis()
            );
            documents.put(id, metadata);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", metadata.getId());
        response.put("title", metadata.getTitle());
        response.put("createdAt", metadata.getCreatedAt());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get all documents (for listing)
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERMISSION_DOCUMENT_LIST')")
    public ResponseEntity<Map<String, Object>> getAllDocuments(Principal principal) {
        Map<String, Object> response = new HashMap<>();
        response.put("documents", documents.values());
        response.put("count", documents.size());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Inner class for document metadata
     */
    private static class DocumentMetadata {
        private final String id;
        private final String title;
        private final long createdAt;

        public DocumentMetadata(String id, String title, long createdAt) {
            this.id = id;
            this.title = title;
            this.createdAt = createdAt;
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public long getCreatedAt() {
            return createdAt;
        }
    }
}

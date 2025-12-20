package com.mmtext.editorserversnapshot.service;

import com.mmtext.editorserversnapshot.model.Document;
import com.mmtext.editorserversnapshot.repo.DocumentRepository;
import com.mmtext.editorserversnapshot.client.grpc.ShareServiceClient;
import com.mmtext.share.grpc.ShareServiceProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing Document entities with synchronous permission creation
 * Implements distributed transaction pattern with Share Service
 */
@Service
public class DocumentService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private ShareServiceClient shareServiceClient;

    /**
     * Create a new document with synchronous permission creation
     * This method implements a distributed transaction pattern where:
     * 1. Document is created in snapshot database
     * 2. Permissions are created synchronously in share service via gRPC
     * 3. If either operation fails, the entire creation is rolled back
     *
     * @param title Document title
     * @param ownerId Document owner ID
     * @return Created document
     * @throws DocumentCreationException if document or permission creation fails
     */
    @Transactional(rollbackFor = {Exception.class, DocumentCreationException.class})
    public Document createDocument(String title, String ownerId) {
        logger.info("Creating new document with synchronous permission creation - User: {}, Title: {}", ownerId, title);

        // Generate unique document ID
        String timestamp = String.valueOf(System.currentTimeMillis());
        String randomPart = UUID.randomUUID().toString().substring(0, 8);
        String docId = "doc-" + timestamp + "-" + randomPart;

        Document document = new Document(docId, title != null ? title : "Untitled Document", ownerId);

        try {
            // Step 1: Save document to snapshot database first
            logger.debug("Step 1: Saving document to snapshot database - docId: {}", docId);
            Document savedDocument = documentRepository.save(document);
            logger.info("Document saved successfully to snapshot database: {}", savedDocument.getDocId());

            // Step 2: Create permissions synchronously via gRPC call
            logger.debug("Step 2: Creating permissions via Share Service - docId: {}", docId);
            ShareServiceProto.CreateDocumentPermissionsResponse permissionResponse =
                shareServiceClient.createDocumentPermissions(savedDocument);

            if (!permissionResponse.getSuccess()) {
                throw new DocumentCreationException(
                    "Permission creation failed for document " + docId + ": " + permissionResponse.getErrorMessage());
            }

            logger.info("Permissions created successfully for document: {} - Permission ID: {}",
                       docId, permissionResponse.getPermissionId());

            // Step 3: Audit log the complete creation
            auditDocumentCreation(savedDocument, permissionResponse);

            // Step 4: Return the successfully created document
            logger.info("Document creation completed successfully - docId: {}, permissionId: {}",
                       docId, permissionResponse.getPermissionId());

            return savedDocument;

        } catch (ShareServiceClient.ShareServiceException e) {
            logger.error("Share Service error during document creation for {}: {}", docId, e.getMessage(), e);
            // Transaction will automatically rollback due to @Transactional
            throw new DocumentCreationException("Failed to create document permissions: " + e.getMessage(), e);

        } catch (DataAccessException e) {
            logger.error("Database error during document creation for {}: {}", docId, e.getMessage(), e);
            // Transaction will automatically rollback due to @Transactional
            throw new DocumentCreationException("Database error during document creation: " + e.getMessage(), e);

        } catch (Exception e) {
            logger.error("Unexpected error during document creation for {}: {}", docId, e.getMessage(), e);
            // Transaction will automatically rollback due to @Transactional
            throw new DocumentCreationException("Unexpected error during document creation: " + e.getMessage(), e);
        }
    }

    /**
     * Create a document without permission creation (for internal use)
     * This method is used when permissions need to be handled separately
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Document createDocumentWithoutPermissions(String title, String ownerId) {
        logger.info("Creating document without permissions - User: {}, Title: {}", ownerId, title);

        // Generate unique document ID
        String timestamp = String.valueOf(System.currentTimeMillis());
        String randomPart = UUID.randomUUID().toString().substring(0, 8);
        String docId = "doc-" + timestamp + "-" + randomPart;

        Document document = new Document(docId, title != null ? title : "Untitled Document", ownerId);

        Document savedDocument = documentRepository.save(document);
        logger.info("Document created without permissions: {}", savedDocument.getDocId());

        return savedDocument;
    }

    /**
     * Add permissions to an existing document
     * Used for scenarios where document was created without permissions
     */
    public boolean addPermissionsToExistingDocument(Document document) {
        logger.info("Adding permissions to existing document: {}", document.getDocId());

        try {
            ShareServiceProto.CreateDocumentPermissionsResponse response =
                shareServiceClient.createDocumentPermissions(document);

            if (response.getSuccess()) {
                logger.info("Successfully added permissions to document: {} - Permission ID: {}",
                           document.getDocId(), response.getPermissionId());
                return true;
            } else {
                logger.error("Failed to add permissions to document {}: {}",
                            document.getDocId(), response.getErrorMessage());
                return false;
            }

        } catch (Exception e) {
            logger.error("Error adding permissions to document {}: {}", document.getDocId(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Audit log document creation with permission details
     */
    private void auditDocumentCreation(Document document, ShareServiceProto.CreateDocumentPermissionsResponse permissionResponse) {
        logger.info("Document creation audit - ID: {}, Title: {}, Owner: {}, CreatedAt: {}, PermissionId: {}, GrantedAt: {}",
                    document.getDocId(),
                    document.getTitle(),
                    document.getOwnerId(),
                    document.getCreatedAt(),
                    permissionResponse.getPermissionId(),
                    Instant.ofEpochSecond(
                        permissionResponse.getGrantedAt().getSeconds(),
                        permissionResponse.getGrantedAt().getNanos()
                    ));
    }

    /**
     * Custom exception for document creation failures
     */
    public static class DocumentCreationException extends RuntimeException {
        public DocumentCreationException(String message) {
            super(message);
        }

        public DocumentCreationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Get a document by its docId
     */
    @Transactional(readOnly = true)
    public Optional<Document> getDocumentByDocId(String docId) {
        logger.debug("Fetching document with docId: {}", docId);
        return documentRepository.findByDocId(docId);
    }

    /**
     * Get a document by its database ID
     */
    @Transactional(readOnly = true)
    public Optional<Document> getDocumentById(Long id) {
        logger.debug("Fetching document with ID: {}", id);
        return documentRepository.findById(id);
    }

    /**
     * Update document title
     */
    public Document updateDocumentTitle(String docId, String newTitle) {
        logger.info("Updating document title for docId: {}, new title: {}", docId, newTitle);

        Optional<Document> documentOpt = documentRepository.findByDocId(docId);
        if (documentOpt.isEmpty()) {
            throw new RuntimeException("Document not found with docId: " + docId);
        }

        Document document = documentOpt.get();
        document.setTitle(newTitle);

        Document updatedDocument = documentRepository.save(document);
        logger.info("Document title updated successfully for docId: {}", docId);

        return updatedDocument;
    }

    /**
     * Get all documents owned by a specific user
     */
    @Transactional(readOnly = true)
    public List<Document> getDocumentsByOwner(String ownerId) {
        logger.debug("Fetching documents for owner: {}", ownerId);
        return documentRepository.findByOwnerIdOrderByUpdatedAtDesc(ownerId);
    }

    /**
     * Search documents by title for a specific owner
     */
    @Transactional(readOnly = true)
    public List<Document> searchDocumentsByTitle(String ownerId, String title) {
        logger.debug("Searching documents for owner: {}, title containing: {}", ownerId, title);
        return documentRepository.findByOwnerIdAndTitleContainingIgnoreCase(ownerId, title);
    }

    /**
     * Check if a document exists
     */
    @Transactional(readOnly = true)
    public boolean documentExists(String docId) {
        boolean exists = documentRepository.existsByDocId(docId);
        logger.debug("Document existence check for docId: {} - {}", docId, exists);
        return exists;
    }

    /**
     * Delete a document (hard delete)
     */
    public void deleteDocument(String docId) {
        logger.info("Deleting document with docId: {}", docId);

        Optional<Document> documentOpt = documentRepository.findByDocId(docId);
        if (documentOpt.isEmpty()) {
            throw new RuntimeException("Document not found with docId: " + docId);
        }

        documentRepository.delete(documentOpt.get());
        logger.info("Document deleted successfully with docId: {}", docId);
    }

    /**
     * Get document count for a specific owner
     */
    @Transactional(readOnly = true)
    public long getDocumentCountByOwner(String ownerId) {
        logger.debug("Counting documents for owner: {}", ownerId);
        return documentRepository.countByOwnerId(ownerId);
    }

    /**
     * Get recently created documents
     */
    @Transactional(readOnly = true)
    public List<Document> getRecentlyCreatedDocuments(int hours) {
        Instant since = Instant.now().minusSeconds(hours * 3600L);
        logger.debug("Fetching documents created since: {}", since);
        return documentRepository.findByCreatedAtAfter(since);
    }

    /**
     * Get recently updated documents
     */
    @Transactional(readOnly = true)
    public List<Document> getRecentlyUpdatedDocuments(int hours) {
        Instant since = Instant.now().minusSeconds(hours * 3600L);
        logger.debug("Fetching documents updated since: {}", since);
        return documentRepository.findByUpdatedAtAfter(since);
    }

    /**
     * Get documents by a list of docIds
     */
    @Transactional(readOnly = true)
    public List<Document> getDocumentsByDocIds(List<String> docIds) {
        logger.debug("Fetching {} documents by docIds", docIds.size());
        return documentRepository.findByDocIdIn(docIds);
    }
}
package com.mmtext.editorserversnapshot.service;

import com.mmtext.editorserversnapshot.model.Document;
import com.mmtext.editorserversnapshot.repo.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing Document entities
 */
@Service
@Transactional
public class DocumentService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);

    @Autowired
    private DocumentRepository documentRepository;

    /**
     * Create a new document with the given title and owner
     */
    public Document createDocument(String title, String ownerId) {
        logger.info("Creating new document for user: {}, title: {}", ownerId, title);

        // Generate unique document ID
        String timestamp = String.valueOf(System.currentTimeMillis());
        String randomPart = UUID.randomUUID().toString().substring(0, 8);
        String docId = "doc-" + timestamp + "-" + randomPart;

        Document document = new Document(docId, title != null ? title : "Untitled Document", ownerId);

        Document savedDocument = documentRepository.save(document);
        logger.info("Document created successfully with docId: {}", savedDocument.getDocId());

        return savedDocument;
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
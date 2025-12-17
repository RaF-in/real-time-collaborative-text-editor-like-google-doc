package com.mmtext.editorserversnapshot.repo;

import com.mmtext.editorserversnapshot.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    /**
     * Find a document by its docId
     */
    Optional<Document> findByDocId(String docId);

    /**
     * Check if a document exists by docId
     */
    boolean existsByDocId(String docId);

    /**
     * Find all documents owned by a specific user
     */
    List<Document> findByOwnerId(String ownerId);

    /**
     * Find all documents owned by a specific user ordered by updated date descending
     */
    @Query("SELECT d FROM Document d WHERE d.ownerId = :ownerId ORDER BY d.updatedAt DESC")
    List<Document> findByOwnerIdOrderByUpdatedAtDesc(@Param("ownerId") String ownerId);

    /**
     * Find documents by title containing the given string (case-insensitive)
     */
    @Query("SELECT d FROM Document d WHERE d.ownerId = :ownerId AND LOWER(d.title) LIKE LOWER(CONCAT('%', :title, '%')) ORDER BY d.updatedAt DESC")
    List<Document> findByOwnerIdAndTitleContainingIgnoreCase(@Param("ownerId") String ownerId, @Param("title") String title);

    /**
     * Find all documents created after a specific date
     */
    List<Document> findByCreatedAtAfter(Instant createdAt);

    /**
     * Find all documents updated after a specific date
     */
    List<Document> findByUpdatedAtAfter(Instant updatedAt);

    /**
     * Count documents owned by a specific user
     */
    @Query("SELECT COUNT(d) FROM Document d WHERE d.ownerId = :ownerId")
    long countByOwnerId(@Param("ownerId") String ownerId);

    /**
     * Find all documents with docIds in the given list
     */
    @Query("SELECT d FROM Document d WHERE d.docId IN :docIds")
    List<Document> findByDocIdIn(@Param("docIds") List<String> docIds);
}
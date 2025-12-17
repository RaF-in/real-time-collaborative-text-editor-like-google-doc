package com.mmtext.editorservershare.repo;

import com.mmtext.editorservershare.model.ShareableLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShareableLinkRepository extends JpaRepository<ShareableLink, UUID> {

    Optional<ShareableLink> findByLinkToken(String linkToken);

    List<ShareableLink> findByDocumentIdAndIsActiveTrue(UUID documentId);

    @Query("SELECT sl FROM ShareableLink sl WHERE sl.documentId = :documentId " +
            "AND sl.isActive = true AND (sl.expiresAt IS NULL OR sl.expiresAt > :now)")
    List<ShareableLink> findActiveByDocumentId(
            @Param("documentId") UUID documentId,
            @Param("now") Instant now
    );

    boolean existsByLinkToken(String linkToken);

    @Modifying
    @Query("UPDATE ShareableLink sl SET sl.isActive = false " +
            "WHERE sl.isActive = true AND sl.expiresAt IS NOT NULL AND sl.expiresAt <= :now")
    int deactivateExpiredLinks(@Param("now") Instant now);

    @Query("SELECT COUNT(sl) FROM ShareableLink sl WHERE sl.documentId = :documentId " +
            "AND sl.isActive = true")
    long countActiveByDocumentId(@Param("documentId") UUID documentId);

    void deleteByDocumentId(UUID documentId);
}

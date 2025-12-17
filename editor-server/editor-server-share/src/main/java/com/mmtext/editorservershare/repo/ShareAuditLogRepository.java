package com.mmtext.editorservershare.repo;

import com.mmtext.editorservershare.model.ShareAuditLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface ShareAuditLogRepository extends JpaRepository<ShareAuditLog, UUID> {

    List<ShareAuditLog> findByDocumentIdOrderByCreatedAtDesc(UUID documentId, Pageable pageable);

    List<ShareAuditLog> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @Query("SELECT sal FROM ShareAuditLog sal WHERE sal.documentId = :documentId " +
            "AND sal.createdAt BETWEEN :startDate AND :endDate ORDER BY sal.createdAt DESC")
    List<ShareAuditLog> findByDocumentIdAndDateRange(
            @Param("documentId") UUID documentId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );

    long countByDocumentId(UUID documentId);
}

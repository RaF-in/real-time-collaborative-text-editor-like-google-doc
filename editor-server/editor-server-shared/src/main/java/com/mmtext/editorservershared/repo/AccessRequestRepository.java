package com.mmtext.editorservershared.repo;

import com.mmtext.editorservershared.enums.RequestStatus;
import com.mmtext.editorservershared.model.AccessRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccessRequestRepository extends JpaRepository<AccessRequest, UUID> {

    List<AccessRequest> findByDocumentIdAndStatus(UUID documentId, RequestStatus status);

    List<AccessRequest> findByRequesterIdOrderByRequestedAtDesc(UUID requesterId);

    Optional<AccessRequest> findByDocumentIdAndRequesterIdAndStatus(
            UUID documentId, UUID requesterId, RequestStatus status
    );

    @Query("SELECT ar FROM AccessRequest ar WHERE ar.documentId IN :documentIds " +
            "AND ar.status = :status ORDER BY ar.requestedAt DESC")
    List<AccessRequest> findByDocumentIdInAndStatus(
            @Param("documentIds") List<UUID> documentIds,
            @Param("status") RequestStatus status
    );

    boolean existsByDocumentIdAndRequesterIdAndStatus(
            UUID documentId, UUID requesterId, RequestStatus status
    );

    long countByDocumentIdAndStatus(UUID documentId, RequestStatus status);

    @Query("SELECT COUNT(ar) FROM AccessRequest ar WHERE ar.documentId IN " +
            "(SELECT dp.documentId FROM DocumentPermission dp WHERE dp.userId = :ownerId " +
            "AND dp.permissionLevel = 'OWNER') AND ar.status = :status")
    long countPendingRequestsForOwner(
            @Param("ownerId") UUID ownerId,
            @Param("status") RequestStatus status
    );
}
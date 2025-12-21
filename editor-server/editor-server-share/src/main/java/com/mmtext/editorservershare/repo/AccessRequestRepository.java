package com.mmtext.editorservershare.repo;

import com.mmtext.editorservershare.enums.RequestStatus;
import com.mmtext.editorservershare.model.AccessRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccessRequestRepository extends JpaRepository<AccessRequest, UUID> {

    List<AccessRequest> findByDocumentIdAndStatus(String documentId, RequestStatus status);

    List<AccessRequest> findByRequesterIdOrderByRequestedAtDesc(UUID requesterId);

    Optional<AccessRequest> findByDocumentIdAndRequesterIdAndStatus(
            String documentId, UUID requesterId, RequestStatus status
    );

    @Query("SELECT ar FROM AccessRequest ar WHERE ar.documentId IN :documentIds " +
            "AND ar.status = :status ORDER BY ar.requestedAt DESC")
    List<AccessRequest> findByDocumentIdInAndStatus(
            @Param("documentIds") List<String> documentIds,
            @Param("status") RequestStatus status
    );

    boolean existsByDocumentIdAndRequesterIdAndStatus(
            String documentId, UUID requesterId, RequestStatus status
    );

    long countByDocumentIdAndStatus(String documentId, RequestStatus status);

    @Query("SELECT COUNT(ar) FROM AccessRequest ar WHERE ar.documentId IN " +
            "(SELECT dp.documentId FROM DocumentPermission dp WHERE dp.userId = :ownerId " +
            "AND dp.permissionLevel = 'OWNER') AND ar.status = :status")
    long countPendingRequestsForOwner(
            @Param("ownerId") UUID ownerId,
            @Param("status") RequestStatus status
    );
}
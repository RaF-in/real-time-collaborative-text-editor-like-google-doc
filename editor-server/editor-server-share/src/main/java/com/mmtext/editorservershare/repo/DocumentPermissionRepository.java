package com.mmtext.editorservershare.repo;

import com.mmtext.editorservershare.enums.PermissionLevel;
import com.mmtext.editorservershare.model.DocumentPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface DocumentPermissionRepository extends JpaRepository<DocumentPermission, UUID> {

    Optional<DocumentPermission> findByDocumentIdAndUserId(UUID documentId, UUID userId);

    List<DocumentPermission> findByDocumentId(UUID documentId);

    List<DocumentPermission> findByUserId(UUID userId);

    @Query("SELECT dp FROM DocumentPermission dp WHERE dp.documentId = :documentId " +
            "AND dp.userId = :userId AND dp.permissionLevel IN :levels")
    Optional<DocumentPermission> findByDocumentIdAndUserIdAndPermissionLevelIn(
            @Param("documentId") UUID documentId,
            @Param("userId") UUID userId,
            @Param("levels") Set<PermissionLevel> levels
    );

    @Query("SELECT CASE WHEN COUNT(dp) > 0 THEN true ELSE false END " +
            "FROM DocumentPermission dp WHERE dp.documentId = :documentId " +
            "AND dp.userId = :userId AND dp.permissionLevel = :level")
    boolean existsByDocumentIdAndUserIdAndPermissionLevel(
            @Param("documentId") UUID documentId,
            @Param("userId") UUID userId,
            @Param("level") PermissionLevel level
    );

    boolean existsByDocumentIdAndUserId(UUID documentId, UUID userId);

    void deleteByDocumentIdAndUserId(UUID documentId, UUID userId);

    void deleteByDocumentId(UUID documentId);

    long countByDocumentId(UUID documentId);

    @Query("SELECT dp.userId FROM DocumentPermission dp WHERE dp.documentId = :documentId " +
            "AND dp.permissionLevel = :level")
    List<UUID> findUserIdsByDocumentIdAndPermissionLevel(
            @Param("documentId") UUID documentId,
            @Param("level") PermissionLevel level
    );

    boolean existsByDocumentId(UUID documentUuid);
}
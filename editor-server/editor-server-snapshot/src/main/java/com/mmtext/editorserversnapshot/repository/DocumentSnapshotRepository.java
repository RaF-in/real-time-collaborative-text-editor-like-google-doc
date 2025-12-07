package com.mmtext.editorserversnapshot.repository;


import com.mmtext.editorserversnapshot.model.DocumentSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentSnapshotRepository extends JpaRepository<DocumentSnapshot, Long> {

    List<DocumentSnapshot> findByDocIdAndActiveOrderByFractionalPosition(String docId, Boolean active);

    Optional<DocumentSnapshot> findByDocIdAndFractionalPositionAndActive(
            String docId, String fractionalPosition, Boolean active);

    @Modifying
    @Query("UPDATE DocumentSnapshot s SET s.active = false WHERE s.docId = :docId AND s.fractionalPosition = :position")
    void deactivateByDocIdAndPosition(@Param("docId") String docId, @Param("position") String position);

    // Method for gap detection
    @Query("SELECT ds FROM DocumentSnapshot ds WHERE ds.docId = :docId AND ds.serverId = :serverId ORDER BY ds.serverSeqNum")
    List<DocumentSnapshot> findByDocIdAndServerIdOrderByServerSeqNum(@Param("docId") String docId,
                                                                     @Param("serverId") String serverId);

    // Check for duplicate operations
    boolean existsByDocIdAndServerIdAndServerSeqNum(String docId, String serverId, Long serverSeqNum);
}

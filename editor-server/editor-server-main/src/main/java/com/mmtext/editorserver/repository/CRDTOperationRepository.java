package com.mmtext.editorserver.repository;// src/main/java/com/editor/repository/CRDTOperationRepository.java

import com.mmtext.editorserver.model.CRDTOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CRDTOperationRepository extends JpaRepository<CRDTOperation, Long> {

    List<CRDTOperation> findByDocIdAndServerIdAndServerSeqNumBetweenOrderByServerSeqNum(
            String docId, String serverId, Long startSeq, Long endSeq);

    @Query("SELECT MAX(o.serverSeqNum) FROM CRDTOperation o WHERE o.docId = :docId AND o.serverId = :serverId")
    Long findMaxServerSeqNum(@Param("docId") String docId, @Param("serverId") String serverId);

    List<CRDTOperation> findByDocIdOrderByTimestamp(String docId);
}
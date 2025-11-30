package com.mmtext.editorserversnapshot.repository;


import com.mmtext.editorserversnapshot.model.VersionVector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VersionVectorRepository extends JpaRepository<VersionVector, Long> {

    List<VersionVector> findByDocId(String docId);

    Optional<VersionVector> findByDocIdAndServerId(String docId, String serverId);
}
package com.mmtext.editorserversnapshot.service;

import com.mmtext.editorserversnapshot.dto.CRDTOperation;
import com.mmtext.editorserversnapshot.model.DocumentSnapshot;
import com.mmtext.editorserversnapshot.model.VersionVector;
import com.mmtext.editorserversnapshot.repository.DocumentSnapshotRepository;
import com.mmtext.editorserversnapshot.repository.VersionVectorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages document snapshots and version vectors using two-phase commit pattern
 *
 * Phase 1: Prepare - Insert into document_snapshots
 * Phase 2: Commit - Update version_vectors
 *
 * This ensures consistency between snapshot and version vector tables
 */
@Service
public class SnapshotService {

    private static final Logger logger = LoggerFactory.getLogger(SnapshotService.class);

    private final DocumentSnapshotRepository snapshotRepository;
    private final VersionVectorRepository versionVectorRepository;

    public SnapshotService(DocumentSnapshotRepository snapshotRepository,
                           VersionVectorRepository versionVectorRepository) {
        this.snapshotRepository = snapshotRepository;
        this.versionVectorRepository = versionVectorRepository;
    }

    /**
     * Apply operation to snapshot using two-phase commit
     *
     * Phase 1 (Prepare): Modify document snapshot
     * Phase 2 (Commit): Update version vector
     */
    @Transactional
    public void applyOperationToSnapshot(CRDTOperation operation) {
        String docId = operation.getDocId();
        String position = operation.getFractionalPosition();

        try {
            // PHASE 1: Prepare - Update document snapshot
            if ("INSERT".equals(operation.getOperationType())) {
                handleInsert(operation);
            } else if ("DELETE".equals(operation.getOperationType())) {
                handleDelete(docId, position);
            }

            // PHASE 2: Commit - Update version vector
            updateVersionVector(docId, operation.getServerId(), operation.getServerSeqNum());

            logger.info("Applied operation to snapshot - Doc: {}, Server: {}, Seq: {}, Type: {}",
                    docId, operation.getServerId(), operation.getServerSeqNum(),
                    operation.getOperationType());

        } catch (Exception e) {
            logger.error("Error applying operation to snapshot", e);
            throw new RuntimeException("Snapshot update failed", e);
        }
    }

    /**
     * Handle INSERT operation
     */
    private void handleInsert(CRDTOperation operation) {
        DocumentSnapshot snapshot = new DocumentSnapshot(
                operation.getDocId(),
                operation.getFractionalPosition(),
                operation.getCharacter(),
                operation.getServerId(),
                operation.getServerSeqNum()
        );

        snapshotRepository.save(snapshot);
        logger.debug("Inserted character '{}' at position {}",
                operation.getCharacter(), operation.getFractionalPosition());
    }

    /**
     * Handle DELETE operation (logical delete by marking inactive)
     */
    private void handleDelete(String docId, String position) {
        snapshotRepository.deactivateByDocIdAndPosition(docId, position);
        logger.debug("Deleted character at position {}", position);
    }

    /**
     * Update version vector (Phase 2 of 2PC)
     */
    private void updateVersionVector(String docId, String serverId, Long seqNum) {
        Optional<VersionVector> existing = versionVectorRepository
                .findByDocIdAndServerId(docId, serverId);

        if (existing.isPresent()) {
            VersionVector vv = existing.get();
            vv.setSequenceNumber(seqNum);
            vv.setUpdatedAt(java.time.Instant.now());
            versionVectorRepository.save(vv);
        } else {
            VersionVector vv = new VersionVector(docId, serverId, seqNum);
            versionVectorRepository.save(vv);
        }
    }

    /**
     * Get current document snapshot (active characters only)
     */
    public List<DocumentSnapshot> getDocumentSnapshot(String docId) {
        return snapshotRepository.findByDocIdAndActiveOrderByFractionalPosition(docId, true);
    }

    /**
     * Get version vector for a document
     * Returns map of serverId -> sequenceNumber
     */
    public Map<String, Long> getVersionVector(String docId) {
        List<VersionVector> vectors = versionVectorRepository.findByDocId(docId);
        return vectors.stream()
                .collect(Collectors.toMap(
                        VersionVector::getServerId,
                        VersionVector::getSequenceNumber
                ));
    }

    /**
     * Get document content as string
     */
    public String getDocumentContent(String docId) {
        List<DocumentSnapshot> snapshots = getDocumentSnapshot(docId);
        StringBuilder content = new StringBuilder();

        for (DocumentSnapshot snapshot : snapshots) {
            if (snapshot.getCharacter() != null) {
                content.append(snapshot.getCharacter());
            }
        }

        return content.toString();
    }

    /**
     * Initialize document with empty snapshot and version vector
     */
    @Transactional
    public void initializeDocument(String docId) {
        // Check if document already exists
        Map<String, Long> existingVector = getVersionVector(docId);
        if (!existingVector.isEmpty()) {
            logger.info("Document {} already initialized", docId);
            return;
        }

        logger.info("Initializing new document: {}", docId);
        // Document starts empty - version vectors will be created as operations arrive
    }

    /**
     * Calculate missing operations based on client's version vector
     */
    public Map<String, List<Long>> calculateMissingOperations(String docId,
                                                              Map<String, Long> clientVector) {
        Map<String, Long> serverVector = getVersionVector(docId);
        Map<String, List<Long>> missingOps = new HashMap<>();

        for (Map.Entry<String, Long> entry : serverVector.entrySet()) {
            String serverId = entry.getKey();
            Long serverSeq = entry.getValue();
            Long clientSeq = clientVector.getOrDefault(serverId, 0L);

            if (serverSeq > clientSeq) {
                List<Long> range = new ArrayList<>();
                range.add(clientSeq + 1); // from
                range.add(serverSeq);     // to
                missingOps.put(serverId, range);

                logger.info("Client missing operations - Server: {}, Range: {}-{}",
                        serverId, clientSeq + 1, serverSeq);
            }
        }

        return missingOps;
    }
}

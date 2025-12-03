package com.mmtext.editorservermain.service;

import com.mmtext.editorservermain.model.CRDTOperation;
import com.mmtext.editorservermain.repository.CRDTOperationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Core CRDT service managing operation sequence numbers and coordination
 */
@Service
public class CRDTService {

    private static final Logger logger = LoggerFactory.getLogger(CRDTService.class);

    private final CRDTOperationRepository operationRepository;
    private final OperationBufferService bufferService;
    private final FractionalIndexService fractionalIndexService;

    @Value("${editor.server.id}")
    private String serverId;

    // In-memory sequence number generator per document
    private final ConcurrentHashMap<String, AtomicLong> sequenceGenerators = new ConcurrentHashMap<>();

    public CRDTService(CRDTOperationRepository operationRepository,
                       OperationBufferService bufferService,
                       FractionalIndexService fractionalIndexService) {
        this.operationRepository = operationRepository;
        this.bufferService = bufferService;
        this.fractionalIndexService = fractionalIndexService;
    }

    /**
     * Process incoming operation from client
     */
    @Transactional
    public CRDTOperation processOperation(CRDTOperation operation) {
        // Assign server ID and sequence number
        operation.setServerId(serverId);
        operation.setServerSeqNum(getNextSequenceNumber(operation.getDocId()));

        logger.info("Processing operation - Doc: {}, Server: {}, Seq: {}, Type: {}, Pos: {}",
                operation.getDocId(),
                operation.getServerId(),
                operation.getServerSeqNum(),
                operation.getOperationType(),
                operation.getFractionalPosition());

        // Add to buffer (will be flushed based on buffer strategy)
        bufferService.addOperation(operation);

        return operation;
    }

    /**
     * Get next sequence number for a document on this server
     */
    private Long getNextSequenceNumber(String docId) {
        AtomicLong generator = sequenceGenerators.computeIfAbsent(docId, key -> {
            // Initialize from database
            Long maxSeq = operationRepository.findMaxServerSeqNum(docId, serverId);
            long startSeq = (maxSeq != null) ? maxSeq : 0L;
            logger.info("Initializing sequence generator for doc: {}, server: {}, starting at: {}",
                    docId, serverId, startSeq);
            return new AtomicLong(startSeq);
        });

        return generator.incrementAndGet();
    }

    /**
     * Fetch missing operations for a client
     * Used when client's version vector is behind
     */
    public List<CRDTOperation> fetchMissingOperations(String docId, String serverId,
                                                      Long fromSeq, Long toSeq) {
        logger.info("Fetching missing operations - Doc: {}, Server: {}, Range: {}-{}",
                docId, serverId, fromSeq, toSeq);

        return operationRepository
                .findByDocIdAndServerIdAndServerSeqNumBetweenOrderByServerSeqNum(
                        docId, serverId, fromSeq, toSeq);
    }

    /**
     * Get all operations for a document (for initial load)
     */
    public List<CRDTOperation> getAllOperations(String docId) {
        return operationRepository.findByDocIdOrderByTimestamp(docId);
    }

    /**
     * Generate fractional index for new character insertion
     */
    public String generatePosition(String before, String after) {
        return fractionalIndexService.generateIndexBetween(before, after);
    }
}
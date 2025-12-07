package com.mmtext.editorserversnapshot.service;

import com.mmtext.editorserversnapshot.dto.CRDTOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Listens to Debezium CDC events from Kafka
 *
 * Debezium captures changes from PostgreSQL WAL and publishes to Kafka
 * This service consumes those events and applies them to the snapshot database
 *
 * Event Flow:
 * 1. Operation saved to primary DB (crdt_operations table)
 * 2. Debezium captures INSERT event
 * 3. Event published to Kafka topic
 * 4. This listener consumes event
 * 5. Applies operation to snapshot database using 2PC
 */
@Service
public class DebeziumChangeListener {

    private static final Logger logger = LoggerFactory.getLogger(DebeziumChangeListener.class);

    private final SnapshotService snapshotService;
    private final ObjectMapper objectMapper;

    // Track processed operations for idempotency
    private final java.util.concurrent.ConcurrentMap<String, Long> lastProcessedSeq =
        new java.util.concurrent.ConcurrentHashMap<>();

    public DebeziumChangeListener(SnapshotService snapshotService, ObjectMapper objectMapper) {
        this.snapshotService = snapshotService;
        this.objectMapper = objectMapper;
    }

    /**
     * Listen to Debezium CDC events - single message version
     */
    @KafkaListener(
            topics = "${debezium.topic.name:editor-server-main.public.crdt_operation_outbox_events}",
            groupId = "${debezium.consumer.group:editor-server-snapshot-cdc-consumer}"
    )
    public void handleCDCEvent(@Payload String message,
                              @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                              @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                              @Header(KafkaHeaders.OFFSET) long offset) {
        logger.debug("Received CDC event from topic: {}, partition: {}, offset: {}", topic, partition, offset);

        try {
            // Log raw message for debugging
            logger.trace("Raw CDC message: {}", message);

            JsonNode event = objectMapper.readTree(message);

            // Debezium event structure
            String operation = event.path("op").asText(); // c=create, u=update, d=delete

            if (!operation.isEmpty()) {
                logger.debug("CDC operation type: {}", operation);

                if ("c".equals(operation) || "r".equals(operation)) { // INSERT or READ (snapshot) operation
                    JsonNode after = event.path("after");
                    if (after.isMissingNode() || after.isNull()) {
                        // For initial snapshot (r operation), data might be at the root
                        after = event;
                    }

                    CRDTOperation crdtOp = parseCRDTOperation(after);

                    if (crdtOp != null) {
                        // Check if already processed (idempotency)
                        String operationKey = crdtOp.getDocId() + ":" + crdtOp.getServerId();
                        Long lastSeq = lastProcessedSeq.get(operationKey);

                        if (lastSeq != null && lastSeq >= crdtOp.getServerSeqNum()) {
                            logger.debug("Skipping already processed operation - Doc: {}, Server: {}, Seq: {}",
                                crdtOp.getDocId(), crdtOp.getServerId(), crdtOp.getServerSeqNum());
                            return;
                        }

                        logger.info("Processing CRDT operation - Doc: {}, Server: {}, Seq: {}, Type: {}",
                            crdtOp.getDocId(), crdtOp.getServerId(), crdtOp.getServerSeqNum(),
                            crdtOp.getOperationType());

                        if (!crdtOp.getProcessed()) {
                            // Apply to snapshot using two-phase commit
                            snapshotService.applyOperationToSnapshot(crdtOp);

                            // Update last processed sequence
                            lastProcessedSeq.put(operationKey, crdtOp.getServerSeqNum());

                            logger.info("Successfully applied operation to snapshot - Doc: {}, Server: {}, Seq: {}",
                                crdtOp.getDocId(), crdtOp.getServerId(), crdtOp.getServerSeqNum());
                        }
                    } else {
                        logger.warn("Failed to parse CRDT operation from CDC event");
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Error processing CDC event. Message: {}", message, e);
            // In production, implement dead-letter queue or retry mechanism
            // For now, we'll log and continue to prevent stopping the consumer
        }
    }

    /**
     * Batch version for handling multiple messages
     */
    public void handleBatchCDCEvents(List<String> messages) {
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        logger.info("Processing batch of {} CDC events", messages.size());

        for (String message : messages) {
            try {
                // Process each message individually
                handleCDCEvent(message, "", 0, 0L);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
                logger.error("Failed to process message in batch. Success: {}, Failures: {}",
                    successCount.get(), failureCount.get(), e);
            }
        }

        logger.info("Batch processing complete. Success: {}, Failures: {}",
            successCount.get(), failureCount.get());
    }

    /**
     * Parse Debezium event payload to CRDTOperation
     */
    private CRDTOperation parseCRDTOperation(JsonNode node) {
        try {
            // Extract the payload field which contains the actual CRDT operation
            JsonNode payloadNode = node.path("payload");

            // If payload is a string, parse it as JSON
            if (payloadNode.isTextual()) {
                payloadNode = objectMapper.readTree(payloadNode.asText());
            }

            // If no payload node, try to parse directly from the current node
            if (payloadNode.isMissingNode() || payloadNode.isNull()) {
                payloadNode = node;
            }

            CRDTOperation op = new CRDTOperation();

            // Parse id as String (UUID) or Long
            JsonNode idNode = payloadNode.path("id");
            if (idNode.isTextual()) {
                // It's a UUID string, set to null as we don't need the ID
                op.setId(null);
            } else {
                op.setId(idNode.asLong());
            }

            op.setDocId(payloadNode.path("doc_id").asText());
            op.setUserId(payloadNode.path("user_id").asText());
            op.setServerId(payloadNode.path("server_id").asText());
            op.setOperationType(payloadNode.path("operation_type").asText());
            op.setCharacter(payloadNode.path("character").asText());
            op.setFractionalPosition(payloadNode.path("fractional_position").asText());
            op.setServerSeqNum(payloadNode.path("server_seq_num").asLong());
            op.setProcessed(payloadNode.path("processed").asBoolean(false));

            // Parse timestamp - could be ISO string or epoch millis
            JsonNode timestampNode = payloadNode.path("timestamp");
            if (timestampNode.isTextual()) {
                // ISO timestamp string
                op.setTimestamp(java.time.Instant.parse(timestampNode.asText()));
            } else {
                // Epoch millis
                op.setTimestamp(java.time.Instant.ofEpochMilli(timestampNode.asLong()));
            }

            return op;

        } catch (Exception e) {
            logger.error("Error parsing CRDT operation from CDC event. Node content: {}", node.toString(), e);
            return null;
        }
    }
}
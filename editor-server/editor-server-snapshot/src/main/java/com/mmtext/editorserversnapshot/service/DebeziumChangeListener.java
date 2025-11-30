package com.mmtext.editorserversnapshot.service;


import com.mmtext.editorserversnapshot.dto.CRDTOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

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

    public DebeziumChangeListener(SnapshotService snapshotService, ObjectMapper objectMapper) {
        this.snapshotService = snapshotService;
        this.objectMapper = objectMapper;
    }

    /**
     * Listen to Debezium CDC events
     *
     * Topic format: {server-name}.{database}.{table}
     * Example: postgres-primary.editor_db.crdt_operations
     */
    @KafkaListener(
            topics = "${debezium.topic.name:postgres-primary.editor_db.crdt_operations}",
            groupId = "${debezium.consumer.group:editor-cdc-consumer}"
    )
    public void handleCDCEvent(String message) {
        try {
            logger.debug("Received CDC event: {}", message);

            JsonNode event = objectMapper.readTree(message);

            // Debezium event structure
            String operation = event.path("op").asText(); // c=create, u=update, d=delete

            if ("c".equals(operation)) { // INSERT operation
                JsonNode after = event.path("after");
                CRDTOperation crdtOp = parseCRDTOperation(after);

                if (crdtOp != null && !crdtOp.getProcessed()) {
                    logger.info("Processing CDC event - Doc: {}, Server: {}, Seq: {}",
                            crdtOp.getDocId(), crdtOp.getServerId(), crdtOp.getServerSeqNum());

                    // Apply to snapshot using two-phase commit
                    snapshotService.applyOperationToSnapshot(crdtOp);

                    // Mark as processed (optional - could be done in snapshot service)
                    // This prevents reprocessing the same operation
                }
            }

        } catch (Exception e) {
            logger.error("Error processing CDC event", e);
            // In production, implement dead-letter queue or retry mechanism
        }
    }

    /**
     * Parse Debezium event payload to CRDTOperation
     */
    private CRDTOperation parseCRDTOperation(JsonNode node) {
        try {
            CRDTOperation op = new CRDTOperation();

            op.setId(node.path("id").asLong());
            op.setDocId(node.path("doc_id").asText());
            op.setUserId(node.path("user_id").asText());
            op.setServerId(node.path("server_id").asText());
            op.setOperationType(node.path("operation_type").asText());
            op.setCharacter(node.path("character").asText());
            op.setFractionalPosition(node.path("fractional_position").asText());
            op.setServerSeqNum(node.path("server_seq_num").asLong());
            op.setProcessed(node.path("processed").asBoolean(false));

            // Parse timestamp
            long timestampMicros = node.path("timestamp").asLong();
            op.setTimestamp(java.time.Instant.ofEpochMilli(timestampMicros / 1000));

            return op;

        } catch (Exception e) {
            logger.error("Error parsing CRDT operation from CDC event", e);
            return null;
        }
    }
}

/**
 * NOTE: To use this Debezium integration, you need to:
 *
 * 1. Add Kafka dependencies to pom.xml:
 *    <dependency>
 *        <groupId>org.springframework.kafka</groupId>
 *        <artifactId>spring-kafka</artifactId>
 *    </dependency>
 *
 * 2. Configure Kafka in application.yml:
 *    spring:
 *      kafka:
 *        bootstrap-servers: kafka:9092
 *        consumer:
 *          group-id: editor-cdc-consumer
 *          auto-offset-reset: earliest
 *          key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
 *          value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
 *
 * 3. Deploy Debezium PostgreSQL connector with configuration:
 * {
 *   "name": "postgres-connector",
 *   "config": {
 *     "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
 *     "database.hostname": "postgres-primary",
 *     "database.port": "5432",
 *     "database.user": "editor_user",
 *     "database.password": "editor_pass",
 *     "database.dbname": "editor_db",
 *     "database.server.name": "postgres-primary",
 *     "table.include.list": "public.crdt_operations",
 *     "plugin.name": "pgoutput"
 *   }
 * }
 */
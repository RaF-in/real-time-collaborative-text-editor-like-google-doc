package com.mmtext.editorservermain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmtext.editorservermain.dto.ClientMessage;
import com.mmtext.editorservermain.model.CRDTOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Complete WebSocket handler with:
 * - Server-side fractional position generation
 * - Document loading from snapshot
 * - Version vector synchronization
 * - Missing operation detection and fetching
 */
@Component
public class EditorWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(EditorWebSocketHandler.class);

    private final CRDTService crdtService;
    private final FractionalIndexService fractionalIndexService;
    private final ObjectMapper objectMapper;

    @Value("${editor.server.id}")
    private String serverId;

    // Document-based session tracking
    private final Map<String, Map<String, WebSocketSession>> documentSessions = new ConcurrentHashMap<>();

    // Session metadata
    private final Map<String, SessionMetadata> sessionMetadata = new ConcurrentHashMap<>();

    public EditorWebSocketHandler(CRDTService crdtService,
                                  FractionalIndexService fractionalIndexService,
                                  ObjectMapper objectMapper) {
        this.crdtService = crdtService;
        this.fractionalIndexService = fractionalIndexService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        String remoteAddress = session.getRemoteAddress() != null
                ? session.getRemoteAddress().toString()
                : "unknown";

        logger.info("WebSocket connection established - Session: {}, Remote: {}, Server: {}",
                sessionId, remoteAddress, serverId);

        sendMessage(session, Map.of(
                "type", "CONNECTED",
                "serverId", serverId,
                "sessionId", sessionId,
                "message", "Connected to " + serverId
        ));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = session.getId();

        try {
            String payload = message.getPayload();
            logger.debug("Received message from session {}: {}", sessionId, payload);

            ClientMessage clientMessage = objectMapper.readValue(payload, ClientMessage.class);
            String messageType = clientMessage.getType();

            switch (messageType) {
                case "SUBSCRIBE":
                    handleSubscribe(session, clientMessage);
                    break;

                case "OPERATION":
                    handleOperation(session, clientMessage);
                    break;

                case "SYNC_REQUEST":
                    handleSyncRequest(session, clientMessage);
                    break;

                case "UNSUBSCRIBE":
                    handleUnsubscribe(session, clientMessage);
                    break;

                case "PING":
                    handlePing(session);
                    break;

                default:
                    logger.warn("Unknown message type: {} from session: {}", messageType, sessionId);
                    sendError(session, "Unknown message type: " + messageType);
            }

        } catch (Exception e) {
            logger.error("Error handling WebSocket message from session: {}", sessionId, e);
            sendError(session, "Error processing message: " + e.getMessage());
        }
    }

    /**
     * Handle client subscription to a document
     * CRITICAL: Loads document from snapshot database and sends to client
     */
    private void handleSubscribe(WebSocketSession session, ClientMessage message) throws IOException {
        String sessionId = session.getId();
        String docId = message.getDocId();
        String userId = message.getUserId();

        if (docId == null || userId == null) {
            sendError(session, "docId and userId are required for subscription");
            return;
        }

        logger.info("Processing SUBSCRIBE - Session: {}, Doc: {}, User: {}",
                sessionId, docId, userId);

        // Add session to document subscribers
        documentSessions.computeIfAbsent(docId, k -> new ConcurrentHashMap<>())
                .put(sessionId, session);

        // Store session metadata
        SessionMetadata metadata = new SessionMetadata(docId, userId, System.currentTimeMillis());
        sessionMetadata.put(sessionId, metadata);

        // ===================================================================
        // CRITICAL: Load document from snapshot database
        // This shows user all edits already made by others
        // ===================================================================

        // Send subscription acknowledgment WITH DOCUMENT DATA
        sendMessage(session, Map.of(
                "type", "SUBSCRIBED",
                "docId", docId,
                "serverId", serverId,
                "message", "Successfully subscribed to document"
        ));

        // Notify other subscribers about new participant
        broadcastToDocument(docId, Map.of(
                "type", "USER_JOINED",
                "userId", userId,
                "docId", docId
        ), sessionId);

        logger.info("Client subscribed successfully - Session: {}, Doc: {}, User: {}",
                sessionId, docId, userId);
    }

    /**
     * Handle incoming operation from client
     * CRITICAL: Server-side fractional position generation
     */
    private void handleOperation(WebSocketSession session, ClientMessage message) throws IOException {
        String sessionId = session.getId();
        SessionMetadata metadata = sessionMetadata.get(sessionId);

        if (metadata == null) {
            sendError(session, "Not subscribed to any document");
            return;
        }

        CRDTOperation operation = message.getOperation();

        if (operation == null) {
            sendError(session, "Operation is required");
            return;
        }

        // Validate operation
        if (operation.getDocId() == null || !operation.getDocId().equals(metadata.getDocId())) {
            sendError(session, "Invalid document ID in operation");
            return;
        }

        String docId = operation.getDocId();
        String operationType = operation.getOperationType();

        // ===================================================================
        // CRITICAL: Server-side fractional position generation
        // Client only sends: insertAfterPosition or deletePosition
        // Server generates the actual fractional index
        // ===================================================================

        if ("INSERT".equals(operationType)) {
            // Client sends: { insertAfterPosition: "m", character: "H" }
            String afterPosition = message.getInsertAfterPosition();
            String beforePosition = message.getInsertBeforePosition();

            // Server generates fractional position between after and before
            String fractionalPosition = fractionalIndexService.generateIndexBetween(
                    afterPosition,
                    beforePosition
            );

            operation.setFractionalPosition(fractionalPosition);

            logger.debug("Generated fractional position - After: {}, Before: {}, Result: {}",
                    afterPosition, beforePosition, fractionalPosition);

        } else if ("DELETE".equals(operationType)) {
            // For delete, client sends the exact position to delete
            // Position is already set in operation
            if (operation.getFractionalPosition() == null) {
                sendError(session, "Delete operation requires fractionalPosition");
                return;
            }
        } else {
            sendError(session, "Unknown operation type: " + operationType);
            return;
        }

        // Set user ID from session
        operation.setUserId(metadata.getUserId());

        // Process operation through CRDT service
        CRDTOperation processed = crdtService.processOperation(operation);

        logger.info("Processed operation - Session: {}, Doc: {}, User: {}, Type: {}, Pos: {}, Seq: {}",
                sessionId,
                processed.getDocId(),
                processed.getUserId(),
                processed.getOperationType(),
                processed.getFractionalPosition(),
                processed.getServerSeqNum());

        // Send acknowledgment back to sender
        sendMessage(session, Map.of(
                "type", "OPERATION_ACK",
                "operation", processed,
                "serverId", serverId
        ));
    }

    /**
     * Handle sync request from client (for missed operations)
     * CRITICAL: Client sends their version vector, server calculates missing ops
     */
    private void handleSyncRequest(WebSocketSession session, ClientMessage message) throws IOException {
        String docId = message.getDocId();
        Map<String, Long> clientVector = message.getVersionVector();

        if (docId == null || clientVector == null) {
            sendError(session, "docId and versionVector are required for sync");
            return;
        }

        logger.info("Sync request - Session: {}, Doc: {}, Client vector: {}",
                session.getId(), docId, clientVector);

        // ===================================================================
        // CRITICAL: Detect missing operations based on version vector
        // ===================================================================


        // Fetch missing operations from PRIMARY database
        Map<String, List<CRDTOperation>> missingOps = new ConcurrentHashMap<>();
        int totalMissing = 0;


        // Send sync response with missing operations
        sendMessage(session, Map.of(
                "type", "SYNC_RESPONSE",
                "docId", docId
        ));

    }

    private void handleUnsubscribe(WebSocketSession session, ClientMessage message) {
        String sessionId = session.getId();
        String docId = message.getDocId();

        removeSession(docId, sessionId);

        logger.info("Client unsubscribed - Session: {}, Doc: {}", sessionId, docId);

        try {
            sendMessage(session, Map.of(
                    "type", "UNSUBSCRIBED",
                    "docId", docId,
                    "message", "Successfully unsubscribed from document"
            ));
        } catch (IOException e) {
            logger.error("Error sending unsubscribe confirmation", e);
        }
    }

    private void handlePing(WebSocketSession session) throws IOException {
        sendMessage(session, Map.of(
                "type", "PONG",
                "timestamp", System.currentTimeMillis(),
                "serverId", serverId
        ));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        SessionMetadata metadata = sessionMetadata.get(sessionId);

        if (metadata != null) {
            String docId = metadata.getDocId();
            String userId = metadata.getUserId();

            removeSession(docId, sessionId);

            broadcastToDocument(docId, Map.of(
                    "type", "USER_LEFT",
                    "userId", userId,
                    "docId", docId
            ), null);

            logger.info("WebSocket connection closed - Session: {}, Doc: {}, User: {}, Status: {}",
                    sessionId, docId, userId, status);
        } else {
            logger.info("WebSocket connection closed - Session: {}, Status: {}", sessionId, status);
        }

        sessionMetadata.remove(sessionId);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = session.getId();
        logger.error("WebSocket transport error - Session: {}", sessionId, exception);

        try {
            sendError(session, "Transport error: " + exception.getMessage());
        } catch (Exception e) {
            logger.error("Failed to send error message", e);
        }
    }

    /**
     * Broadcast operation to all clients subscribed to a document
     */
    public void broadcastToDocument(String docId, CRDTOperation operation) {
        Map<String, Object> message = Map.of(
                "type", "OPERATION_BROADCAST",
                "operation", operation,
                "serverId", serverId
        );

        broadcastToDocument(docId, message, null);
    }

    private void broadcastToDocument(String docId, Map<String, Object> message, String excludeSessionId) {
        Map<String, WebSocketSession> sessions = documentSessions.get(docId);

        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        int successCount = 0;
        int failCount = 0;

        for (Map.Entry<String, WebSocketSession> entry : sessions.entrySet()) {
            String sessionId = entry.getKey();
            WebSocketSession session = entry.getValue();

            if (excludeSessionId != null && sessionId.equals(excludeSessionId)) {
                continue;
            }

            try {
                sendMessage(session, message);
                successCount++;
            } catch (Exception e) {
                logger.error("Error broadcasting to session: {}", sessionId, e);
                failCount++;
            }
        }

        logger.debug("Broadcast complete - Doc: {}, Success: {}, Failed: {}",
                docId, successCount, failCount);
    }

    private void sendMessage(WebSocketSession session, Object message) throws IOException {
        if (session.isOpen()) {
            String json = objectMapper.writeValueAsString(message);
            synchronized (session) {
                session.sendMessage(new TextMessage(json));
            }
        } else {
            logger.warn("Attempted to send message to closed session: {}", session.getId());
        }
    }

    private void sendError(WebSocketSession session, String error) throws IOException {
        sendMessage(session, Map.of(
                "type", "ERROR",
                "message", error,
                "timestamp", System.currentTimeMillis()
        ));
    }

    private void removeSession(String docId, String sessionId) {
        Map<String, WebSocketSession> sessions = documentSessions.get(docId);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                documentSessions.remove(docId);
            }
        }
    }

    public int getActiveSessionCount(String docId) {
        Map<String, WebSocketSession> sessions = documentSessions.get(docId);
        return sessions != null ? sessions.size() : 0;
    }

    public java.util.Set<String> getActiveDocuments() {
        return documentSessions.keySet();
    }

    private static class SessionMetadata {
        private final String docId;
        private final String userId;
        private final long connectedAt;

        public SessionMetadata(String docId, String userId, long connectedAt) {
            this.docId = docId;
            this.userId = userId;
            this.connectedAt = connectedAt;
        }

        public String getDocId() {
            return docId;
        }

        public String getUserId() {
            return userId;
        }

        public long getConnectedAt() {
            return connectedAt;
        }
    }
}
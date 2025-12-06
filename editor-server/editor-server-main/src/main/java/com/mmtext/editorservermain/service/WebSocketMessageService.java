package com.mmtext.editorservermain.service;

import com.mmtext.editorservermain.model.CRDTOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service to handle WebSocket message broadcasting
 * Decouples Redis subscriber from WebSocket handler
 */
@Service
public class WebSocketMessageService {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketMessageService.class);

    private final EditorWebSocketHandler webSocketHandler;

    public WebSocketMessageService(EditorWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    /**
     * Broadcast operation to all WebSocket clients subscribed to the document
     * Excludes the originating session to prevent duplicate messages (only for local operations)
     * For Redis operations, all sessions should receive the broadcast
     */
    public void broadcastOperation(CRDTOperation operation) {
        try {
            int activeCount = webSocketHandler.getActiveSessionCount(operation.getDocId());
            String excludeSessionId = operation.getOriginatingSessionId();

            // Check if this is a local operation (session exists on this server)
            // If session doesn't exist (Redis operation), don't exclude any session
            if (excludeSessionId != null && !isLocalSession(excludeSessionId)) {
                excludeSessionId = null; // Don't exclude any session for Redis operations
            }

            if (activeCount > 0) {
                webSocketHandler.broadcastToDocument(
                    operation.getDocId(),
                    operation,
                    excludeSessionId
                );
                logger.debug("Broadcasted operation to {} clients (excluding session: {}) - Doc: {}, Seq: {}, User: {}",
                        activeCount - (excludeSessionId != null ? 1 : 0),
                        excludeSessionId,
                        operation.getDocId(),
                        operation.getServerSeqNum(),
                        operation.getUserId());
            }
        } catch (Exception e) {
            logger.error("Error broadcasting operation via WebSocket", e);
        }
    }

    /**
     * Check if a session exists on this server
     */
    private boolean isLocalSession(String sessionId) {
        return webSocketHandler.hasSession(sessionId);
    }

    /**
     * Get statistics about active connections
     */
    public java.util.Map<String, Object> getConnectionStats() {
        java.util.Set<String> activeDocs = webSocketHandler.getActiveDocuments();
        int totalSessions = activeDocs.stream()
                .mapToInt(webSocketHandler::getActiveSessionCount)
                .sum();

        return java.util.Map.of(
                "activeDocuments", activeDocs.size(),
                "totalSessions", totalSessions,
                "documents", activeDocs
        );
    }
}
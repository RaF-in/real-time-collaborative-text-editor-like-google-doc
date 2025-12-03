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
     */
    public void broadcastOperation(CRDTOperation operation) {
        try {
            int activeCount = webSocketHandler.getActiveSessionCount(operation.getDocId());

            if (activeCount > 0) {
                webSocketHandler.broadcastToDocument(operation.getDocId(), operation);
                logger.debug("Broadcasted operation to {} clients - Doc: {}, Seq: {}",
                        activeCount, operation.getDocId(), operation.getServerSeqNum());
            }
        } catch (Exception e) {
            logger.error("Error broadcasting operation via WebSocket", e);
        }
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
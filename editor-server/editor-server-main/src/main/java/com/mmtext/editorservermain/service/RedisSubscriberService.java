package com.mmtext.editorservermain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmtext.editorservermain.model.CRDTOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;

@Service
public class RedisSubscriberService implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(RedisSubscriberService.class);

    private final ObjectMapper objectMapper;
    private final WebSocketMessageService webSocketService;

    public RedisSubscriberService(ObjectMapper objectMapper,
                                  WebSocketMessageService webSocketService) {
        this.objectMapper = objectMapper;
        this.webSocketService = webSocketService;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel());
            String body = new String(message.getBody());

            logger.debug("Received message from Redis channel: {}", channel);

            CRDTOperation operation = objectMapper.readValue(body, CRDTOperation.class);

            // Forward to WebSocket clients subscribed to this document
            webSocketService.broadcastOperation(operation);

        } catch (Exception e) {
            logger.error("Error processing Redis message", e);
        }
    }
}

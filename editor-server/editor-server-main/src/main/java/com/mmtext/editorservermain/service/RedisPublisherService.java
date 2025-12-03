package com.mmtext.editorservermain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmtext.editorservermain.model.CRDTOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;



@Service
public class RedisPublisherService {

    private static final Logger logger = LoggerFactory.getLogger(RedisPublisherService.class);
    private static final String CHANNEL_PREFIX = "editor:doc:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisPublisherService(RedisTemplate<String, String> redisTemplate,
                                 ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publish operation to Redis pub/sub channel
     * Channel format: editor:doc:{docId}
     */
    public void publishOperation(CRDTOperation operation) {
        try {
            String channel = CHANNEL_PREFIX + operation.getDocId();
            String message = objectMapper.writeValueAsString(operation);

            redisTemplate.convertAndSend(channel, message);

            logger.debug("Published operation to Redis channel: {}, seq: {}",
                    channel, operation.getServerSeqNum());
        } catch (Exception e) {
            logger.error("Error publishing operation to Redis", e);
            throw new RuntimeException("Failed to publish operation", e);
        }
    }
}

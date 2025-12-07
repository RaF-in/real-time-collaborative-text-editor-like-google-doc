package com.mmtext.editorserversnapshot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka error handling configuration
 */
@Configuration
public class KafkaErrorHandler {

    private static final Logger logger = LoggerFactory.getLogger(KafkaErrorHandler.class);

    /**
     * Dead letter publishing recoverer
     */
    @Bean
    public ConsumerRecordRecoverer deadLetterRecoverer(KafkaTemplate<String, String> kafkaTemplate) {
        return (consumerRecord, exception) -> {
            String deadLetterTopic = consumerRecord.topic() + ".dead";
            String key = consumerRecord.key() != null ? consumerRecord.key().toString() : null;
            String value = consumerRecord.value() != null ? consumerRecord.value().toString() : null;

            logger.error("Sending record to dead letter topic: {}, key: {}, error: {}",
                deadLetterTopic, key, exception.getMessage(), exception);

            try {
                kafkaTemplate.send(deadLetterTopic, key, value);
            } catch (Exception e) {
                logger.error("Failed to send record to dead letter topic", e);
                // Could implement fallback here like writing to file or database
            }
        };
    }

    /**
     * Kafka template for dead letter queue
     */
    @Bean
    public KafkaTemplate<String, String> deadLetterKafkaTemplate() {
        Map<String, Object> props = new HashMap<>();

        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
            "kafka-1:19092,kafka-2:19092,kafka-3:19092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        // Add retries for dead letter publishing
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);

        ProducerFactory<String, String> producerFactory =
            new DefaultKafkaProducerFactory<>(props);

        return new KafkaTemplate<>(producerFactory);
    }
}
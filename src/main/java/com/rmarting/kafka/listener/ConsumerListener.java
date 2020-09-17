package com.rmarting.kafka.listener;

import com.rmarting.kafka.schema.avro.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ConsumerListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerListener.class);

    @KafkaListener(topics = {"messages"})
    public void handleMessages(@Payload Message message,
                               @Headers Map<String, Object> headers,
                               Acknowledgment acknowledgment) {
        LOGGER.info("Received record from Topic-Partition '{}-{}' with Offset '{}' -> Key: '{}' - Value '{}'",
                headers.get(KafkaHeaders.RECEIVED_TOPIC),
                headers.get(KafkaHeaders.RECEIVED_PARTITION_ID),
                headers.get(KafkaHeaders.OFFSET),
                headers.get(KafkaHeaders.MESSAGE_KEY),
                message.get("content"));

        // Commit message
        acknowledgment.acknowledge();
    }

}

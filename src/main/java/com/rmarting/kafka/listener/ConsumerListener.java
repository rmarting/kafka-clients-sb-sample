package com.rmarting.kafka.listener;

import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ConsumerListener {

    private final static Logger LOGGER = LoggerFactory.getLogger(ConsumerListener.class);

    @KafkaListener(topics = {"messages"})
    public void handleMessages(@Payload GenericRecord message, @Headers Map<String, Object> headers) {
        LOGGER.info("Received record from Topic-Partition '{}-{}' with Offset '{}' -> Key: '{}' - Value '{}'",
                headers.get(KafkaHeaders.RECEIVED_TOPIC),
                headers.get(KafkaHeaders.RECEIVED_PARTITION_ID),
                headers.get(KafkaHeaders.OFFSET),
                headers.get(KafkaHeaders.MESSAGE_KEY),
                message.get("content").toString());
    }

}

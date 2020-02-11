package com.jromanmartin.kafka.consumer;

import com.jromanmartin.kafka.model.CustomMessage;
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

    @KafkaListener(
            topics = {"one-topic", "another-topic"},
            groupId = "kafka-clients-sb-sample-group")
    public void handleFarewells(@Payload CustomMessage message, @Headers Map<String, Object> headers) {
        LOGGER.info("Received message: {}. Topic: {}. Partition: {}. Offset: {}", message.getContent(),
                headers.get(KafkaHeaders.RECEIVED_TOPIC), headers.get(KafkaHeaders.RECEIVED_PARTITION_ID),
                headers.get(KafkaHeaders.OFFSET));
    }

}

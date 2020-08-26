package com.jromanmartin.kafka.consumer;

import com.jromanmartin.kafka.model.CustomMessage;
import com.jromanmartin.kafka.schema.CustomAvroMessage;

import org.apache.avro.generic.GenericRecord;
import org.apache.avro.specific.SpecificData;
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
            topics = {"sb-topic2"},
            groupId = "kafka-clients-sb-sample-group")
    public void handleFarewells(@Payload GenericRecord message, @Headers Map<String, Object> headers) {

        // CustomAvroMessage cam = (CustomAvroMessage) SpecificData.get().deepCopy(message.getSchema(), message);

        LOGGER.info("Received message: {}. Topic: {}. Partition: {}. Offset: {}. Schema {}", message.toString(),
                headers.get(KafkaHeaders.RECEIVED_TOPIC), headers.get(KafkaHeaders.RECEIVED_PARTITION_ID),
                headers.get(KafkaHeaders.OFFSET), message.getSchema().getFullName());
    }

}

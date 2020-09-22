package com.rmarting.kafka.listener;

//import com.rmarting.kafka.schema.avro.Message;
import io.smallrye.reactive.messaging.kafka.IncomingKafkaRecord;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// Spring Kafka not longer needed
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.kafka.support.Acknowledgment;
//import org.springframework.kafka.support.KafkaHeaders;
//import org.springframework.messaging.handler.annotation.Headers;
//import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import javax.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.concurrent.CompletionStage;

//@Component
@ApplicationScoped
public class ConsumerListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerListener.class);

    // Spring Kafka not longer needed
//    @KafkaListener(topics = {"messages"})
//    public void handleMessages(@Payload Message message,
//                               @Headers Map<String, Object> headers,
//                               Acknowledgment acknowledgment) {
//        LOGGER.info("Received record from Topic-Partition '{}-{}' with Offset '{}' -> Key: '{}' - Value '{}'",
//                headers.get(KafkaHeaders.RECEIVED_TOPIC),
//                headers.get(KafkaHeaders.RECEIVED_PARTITION_ID),
//                headers.get(KafkaHeaders.OFFSET),
//                headers.get(KafkaHeaders.MESSAGE_KEY),
//                message.get("content"));
//
//        // Commit message
//        acknowledgment.acknowledge();
//    }

    @Incoming("messages")
    public CompletionStage handleMessages(Message message) {
        IncomingKafkaRecord<String, com.rmarting.kafka.schema.avro.Message> incomingKafkaRecord =
                (IncomingKafkaRecord<String, com.rmarting.kafka.schema.avro.Message>) message.unwrap(IncomingKafkaRecord.class);

        LOGGER.info("Received record from Topic-Partition '{}-{}' with Offset '{}' -> Key: '{}' - Value '{}'",
                incomingKafkaRecord.getTopic(),
                incomingKafkaRecord.getPartition(),
                incomingKafkaRecord.getOffset(),
                incomingKafkaRecord.getKey(),
                incomingKafkaRecord.getPayload());

        // Commit message
        return message.ack();
    }

}

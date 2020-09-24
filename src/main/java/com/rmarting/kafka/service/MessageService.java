package com.rmarting.kafka.service;

import com.rmarting.kafka.dto.MessageDTO;
import com.rmarting.kafka.dto.MessageListDTO;
import com.rmarting.kafka.schema.avro.Message;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
// Kafka Templates provided by Reactive System
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.kafka.support.SendResult;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

/**
 * Service for Kafka service which includes a set of primitives to manage events and topics such as:
 * 1. consume events from topic
 * 2. send event to topic
 * 3. subscribe topic
 * <p>
 * Additionally, it only processes messages of type {@link Message}
 *
 * @author rmarting
 */
@Service
@Scope("prototype")
public class MessageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageService.class);

    // Not implemented in Spring DI
    //private ObjectFactory<Producer<String, Message>> producer;
    @Autowired
    Producer<String, Message> producer;

    // Not implemented in Spring DI
    //private ObjectFactory<Consumer<String, Message>> consumer;
    @Autowired
    Consumer<String, Message> consumer;

    // Spring Kafka not longer needed
    // private KafkaTemplate<String, Message> kafkaTemplate;
    @Inject
    @Channel("messages-out")
    Emitter<Message> messageEmitter;

    @Value("${app.consumer.poolTimeout}")
    Long poolTimeout;

//    public MessageService(ObjectFactory<Producer<String, Message>> producer,
//                          ObjectFactory<Consumer<String, Message>> consumer,
//                          KafkaTemplate<String, Message> kafkaTemplate) {
//        this.consumer = consumer;
//        this.producer = producer;
//        this.kafkaTemplate = kafkaTemplate;
//    }

//    public MessageService(Producer<String, Message> producer, Consumer<String, Message> consumer) {
//        this.producer = producer;
//        this.consumer = consumer;
//    }

    public MessageDTO publishSync(final @NotEmpty String topicName, final @NotNull MessageDTO messageDTO) {
        return publishRawMessage(topicName, messageDTO, false);
    }

    public MessageDTO publishAsync(final @NotEmpty String topicName, final @NotNull MessageDTO messageDTO) {
        return publishRawMessage(topicName, messageDTO, true);
    }

    private MessageDTO publishRawMessage(final @NotEmpty String topicName,
                                         final @NotNull MessageDTO messageDTO,
                                         final boolean async) {
        // Message to send
        // TODO Create a Mapper
        Message message = new Message();
        message.setContent(messageDTO.getContent());
        message.setTimestamp(System.currentTimeMillis());

        // Record with a CustomMessage as value
        ProducerRecord<String, Message> record = null;

        if (null == messageDTO.getKey()) {
            // Value as CustomMessage
            record = new ProducerRecord<>(topicName, message);
        } else {
            // Value as CustomMessage
            record = new ProducerRecord<>(topicName, messageDTO.getKey(), message);
        }

        // Local instance (prototype)
        //Producer<String, Message> localProducer = producer.getObject();
        Producer<String, Message> localProducer = producer;

        try {
            if (async) {
                localProducer.send(record, (metadata, exception) -> {
                    LOGGER.info("Record ASYNCHRONOUSLY sent to partition {} with offset {}",
                            metadata.partition(), metadata.offset());

                    // Update model
                    messageDTO.setPartition(metadata.partition());
                    messageDTO.setOffset(metadata.offset());
                    messageDTO.setTimestamp(message.getTimestamp());
                }).get();
            } else {
                RecordMetadata metadata = localProducer.send(record).get();

                LOGGER.info("Record sent to partition {} with offset {}", metadata.partition(), metadata.offset());

                // Update model
                messageDTO.setPartition(metadata.partition());
                messageDTO.setOffset(metadata.offset());
                messageDTO.setTimestamp(message.getTimestamp());
            }
        } catch (ExecutionException e) {
            LOGGER.warn("Execution Error in sending record", e);
        } catch (InterruptedException e) {
            LOGGER.warn("Interrupted Error in sending record", e);
        } finally {
            localProducer.flush();
            localProducer.close();
        }

        return messageDTO;
    }

    public MessageDTO sendMessage(final @NotEmpty String topicName,
                                  final @NotNull MessageDTO messageDTO) {
        // Message to send
        // TODO Create a Mapper
        Message message = new Message();
        message.setContent(messageDTO.getContent());
        message.setTimestamp(System.currentTimeMillis());

        CompletionStage completionStage = messageEmitter.send(message);

        // TODO Get Metadata from completionStage
        completionStage.toCompletableFuture().join();

//        SendResult<String, Message> record = null;
//
//        try {
//            if (null == messageDTO.getKey()) {
//                // Value as CustomMessage
//                record = kafkaTemplate.send(topicName, message).get();
//            } else {
//                // Value as CustomMessage
//                record = kafkaTemplate.send(topicName, messageDTO.getKey(), message).get();
//            }
//
//            LOGGER.info("Record sent to partition {} with offset {}",
//                    record.getRecordMetadata().partition(), record.getRecordMetadata().offset());
//
//            // Update model
//            messageDTO.setPartition(record.getRecordMetadata().partition());
//            messageDTO.setOffset(record.getRecordMetadata().offset());
//        } catch (ExecutionException e) {
//            LOGGER.warn("Execution Error in sending record", e);
//        } catch (InterruptedException e) {
//            LOGGER.warn("Interrupted Error in sending record", e);
//        }

        return messageDTO;
    }

    public MessageListDTO pollEvents(final @NotEmpty String topicName, final Integer partition, final boolean commit) {
        // Response objects
        MessageListDTO messageListDTO = new MessageListDTO();

        // Local instance (prototype)
        //Consumer<String, Message> localConsumer = consumer.getObject();
        Consumer<String, Message> localConsumer = consumer;

        try {
            // Assign to partition defined
            if (null != partition) {
                TopicPartition topicPartition = new TopicPartition(topicName, partition);
                localConsumer.assign(Collections.singletonList(topicPartition));

                LOGGER.info("Consumer assigned to topic {} and partition {}", topicName, partition);
            } else {
                // Subscribe to Topic
                localConsumer.subscribe(Collections.singletonList(topicName));

                LOGGER.info("Consumer subscribed to topic {}", topicName);
            }

            LOGGER.info("Polling records from topic {}", topicName);

            ConsumerRecords<String, Message> consumerRecords = localConsumer.poll(Duration.ofSeconds(poolTimeout));

            LOGGER.info("Polled #{} records from topic {}", consumerRecords.count(), topicName);

            consumerRecords.forEach(record -> {
                MessageDTO messageDTO = new MessageDTO();
                // TODO Create a Mapper
                messageDTO.setTimestamp((Long) record.value().get("timestamp"));
                messageDTO.setContent(record.value().get("content").toString());
                // Record Metadata
                messageDTO.setKey((null != record.key() ? record.key() : null));
                messageDTO.setPartition(record.partition());
                messageDTO.setOffset(record.offset());

                messageListDTO.addCustomMessage(messageDTO);
            });

            // Commit consumption
            if (commit) {
                localConsumer.commitAsync();

                LOGGER.info("Records committed in topic {} from consumer", topicName);
            }
        } finally {
            localConsumer.close();
        }

        return messageListDTO;
    }

}

package com.rmarting.kafka.api;

import com.rmarting.kafka.dto.MessageDTO;
import com.rmarting.kafka.schema.avro.Message;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/producer")
@Tag(name = "producer", description = "Operations to produce messages to a Kafka Cluster")
public class ProducerController {
	
	private final static Logger LOGGER = LoggerFactory.getLogger(ProducerController.class);

	@Autowired
	private ApplicationContext applicationContext;
	
	@Operation(summary = "Send a message synchronously", tags = { "producer"})
    @ApiResponses(value = {
		@ApiResponse(
				responseCode = "200",
				description = "Message sent",
				content = @Content(schema = @Schema(implementation = MessageDTO.class))),
		@ApiResponse(responseCode = "404", description = "Message not sent"),
   		@ApiResponse(responseCode = "500", description = "Internal Server Error")
	})
	@PostMapping(value = "/kafka/{topicName}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<MessageDTO> sendToTopic(
			@Parameter(description = "Topic name", required = true) @PathVariable String topicName,
			@Parameter(description = "Message to send", required = true) @RequestBody MessageDTO messageDTO) {
		// Message to send
		// TODO Create a Mapper
		Message message = new Message();
		message.setContent(messageDTO.getContent());
		message.setTimestamp(System.currentTimeMillis());

		// TODO Refactor to a KafkaFacade class

		// Producer
		Producer<CharSequence, Message> producer = applicationContext.getBean(Producer.class);

		// Record with a CustomMessage as value
		ProducerRecord<CharSequence, Message> record = null;

		if (null == messageDTO.getKey()) {
			// Value as CustomMessage
			record = new ProducerRecord<>(topicName, message);
		} else {
			// Value as CustomMessage
			record = new ProducerRecord<>(topicName, messageDTO.getKey(), message);
		}

		RecordMetadata metadata = null;
		try {			
			metadata = producer.send(record).get();

			LOGGER.info("Record sent to partition {} with offset {}", metadata.partition(), metadata.offset());

			// Update model
			messageDTO.setPartition(metadata.partition());
			messageDTO.setOffset(metadata.offset());
		} catch (ExecutionException e) {
			LOGGER.warn("Execution Error in sending record", e);
		} catch (InterruptedException e) {
			LOGGER.warn("Interrupted Error in sending record", e);
		} finally {
			producer.flush();
			producer.close();
		}

		return ResponseEntity.ok(messageDTO);
	}

	@Operation(summary = "Send a message asynchronously", tags = { "producer"})
	@ApiResponses(value = {
			@ApiResponse(
					responseCode = "200",
					description = "Message sent",
					content = @Content(schema = @Schema(implementation = MessageDTO.class))),
			@ApiResponse(responseCode = "404", description = "Message not sent"),
			@ApiResponse(responseCode = "500", description = "Internal Server Error")
	})
	@PostMapping(value = "/kafka/async/{topicName}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<MessageDTO> sendToTopicAsync(
			@Parameter(description = "Topic name", required = true) @PathVariable String topicName,
			@Parameter(description = "Topic name", required = true) @RequestBody MessageDTO messageDTO) {
		// Message to send
		// TODO Create a Mapper
		Message message = new Message();
		message.setContent(messageDTO.getContent());
		message.setTimestamp(System.currentTimeMillis());

		// TODO Refactor to a KafkaFacade class

		// Producer
		Producer<CharSequence, Message> producer = applicationContext.getBean(Producer.class);

		// Record with a CustomMessage as value
		ProducerRecord<CharSequence, Message> record = null;

		if (null == messageDTO.getKey()) {
			// Value as CustomMessage
			record = new ProducerRecord<>(topicName, message);
		} else {
			// Value as CustomMessage
			record = new ProducerRecord<>(topicName, messageDTO.getKey(), message);
		}

		try {			
			producer.send(record, (metadata, exception) -> {
				if (null != metadata) {
					LOGGER.info("Record sent to partition {} with offset {}", metadata.partition(), metadata.offset());

					// Update model
					messageDTO.setPartition(metadata.partition());
					messageDTO.setOffset(metadata.offset());
				} else {
					LOGGER.warn("Unable to put record into topic", exception);
				}
			});		
		} finally {
			producer.flush();
			producer.close();
		}

		return ResponseEntity.ok(messageDTO);
	}	
	
}

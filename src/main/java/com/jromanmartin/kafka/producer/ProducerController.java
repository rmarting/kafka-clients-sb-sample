package com.jromanmartin.kafka.producer;

import com.jromanmartin.kafka.model.CustomMessage;
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
import org.springframework.stereotype.Controller;
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
				content = @Content(schema = @Schema(implementation = CustomMessage.class))),
		@ApiResponse(responseCode = "404", description = "Message not sent"),
   		@ApiResponse(responseCode = "500", description = "Internal Server Error")
	})
	@PostMapping(value = "/kafka/{topicName}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<CustomMessage> sendToTopic(
			@Parameter(description = "Topic name", required = true) @PathVariable String topicName,
			@Parameter(description = "Message to send", required = true) @RequestBody CustomMessage message) {
		// Prepare message
		message.setTimestamp(System.currentTimeMillis());
		
		// Record with a CustomMessage as value
		ProducerRecord<Long, CustomMessage> record = null;

		if (null == message.getKey()) {
			// Value as CustomMessage
			record = new ProducerRecord<>(topicName, message);
		} else {
			// Value as CustomMessage
			record = new ProducerRecord<>(topicName, message.getKey(), message);
		}

		// Producer
		Producer<Long, CustomMessage> producer = applicationContext.getBean(Producer.class);
		RecordMetadata metadata = null;
		try {			
			metadata = producer.send(record).get();

			LOGGER.info("Record sent to partition {} with offset {}", metadata.partition(), metadata.offset());

			// Update model
			message.setPartition(metadata.partition());
			message.setOffset(metadata.offset());
		} catch (ExecutionException e) {
			LOGGER.warn("Execution Error in sending record", e);
		} catch (InterruptedException e) {
			LOGGER.warn("Interrupted Error in sending record", e);
		} finally {
			producer.flush();
			producer.close();
		}

		return ResponseEntity.ok(message);
	}

	@Operation(summary = "Send a message asynchronously", tags = { "producer"})
	@ApiResponses(value = {
			@ApiResponse(
					responseCode = "200",
					description = "Message sent",
					content = @Content(schema = @Schema(implementation = CustomMessage.class))),
			@ApiResponse(responseCode = "404", description = "Message not sent"),
			@ApiResponse(responseCode = "500", description = "Internal Server Error")
	})
	@PostMapping(value = "/kafka/async/{topicName}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<CustomMessage> sendToTopicAsync(
			@Parameter(description = "Topic name", required = true) @PathVariable String topicName,
			@Parameter(description = "Topic name", required = true) @RequestBody CustomMessage message) {
		// Prepare message
		message.setTimestamp(System.currentTimeMillis());

		// Record with a CustomMessage as value
		ProducerRecord<Long, CustomMessage> record = null;

		if (null == message.getKey()) {
			// Value as CustomMessage
			record = new ProducerRecord<>(topicName, message);
		} else {
			// Value as CustomMessage
			record = new ProducerRecord<>(topicName, message.getKey(), message);
		}

		// Producer
		Producer<Long, CustomMessage> producer = applicationContext.getBean(Producer.class);
		
		try {			
			producer.send(record, (metadata, exception) -> {
				if (null != metadata) {
					LOGGER.info("Record sent to partition {} with offset {}", metadata.partition(), metadata.offset());

					// Update model
					message.setPartition(metadata.partition());
					message.setOffset(metadata.offset());
				} else {
					LOGGER.warn("Unable to put record into topic", exception);
				}
			});		
		} finally {
			producer.flush();
			producer.close();
		}

		return ResponseEntity.ok(message);
	}	
	
}

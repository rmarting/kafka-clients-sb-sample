package com.jromanmartin.kafka.producer;

import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutionException;

import com.jromanmartin.kafka.model.CustomMessage;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Controller
@RequestMapping("/producer")
@Api(value = "producer", description = "Operations to produce messages to a Kafka Cluster")
public class ProducerController {
	
	private final static Logger LOGGER = LoggerFactory.getLogger(ProducerController.class);

	@Autowired
	private ApplicationContext applicationContext;
	
	@ApiOperation(value = "Send a message synchronously", response = CustomMessage.class)
    @ApiResponses(value = {
		@ApiResponse(code = 200, message = "Message sent", response = String.class, responseContainer = "Custom Message with extra information"),
		@ApiResponse(code = 404, message = "Message not sent"),
   		@ApiResponse(code = 500, message = "Internal Server Error")   		
	})	
	@PostMapping(value = "/kafka/{topicName}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<CustomMessage> sendToTopic(
			@ApiParam(name = "topicName", value = "Topic Name") @PathVariable String topicName, 
			@ApiParam(name = "message", value = "Message") @RequestBody CustomMessage message) {
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

	@ApiOperation(value = "Send a message asynchronously", response = CustomMessage.class)
    @ApiResponses(value = {
		@ApiResponse(code = 200, message = "Message sent", response = String.class, responseContainer = "Custom Message with extra information"),
		@ApiResponse(code = 404, message = "Message not sent"),
   		@ApiResponse(code = 500, message = "Internal Server Error")   		
	})	
	@PostMapping(value = "/kafka/async/{topicName}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<CustomMessage> sendToTopicAsync(
			@ApiParam(name = "topicName", value = "Topic Name") @PathVariable String topicName, 
			@ApiParam(name = "message", value = "Message") @RequestBody CustomMessage message) {
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

package com.rmarting.kafka.api;

//import com.jromanmartin.kafka.dto.CustomMessage;
//import com.jromanmartin.kafka.dto.CustomMessageList;
import com.rmarting.kafka.dto.MessageDTO;
import com.rmarting.kafka.dto.MessageListDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/consumer")
@Tag(name = "consumer", description = "Operations to consume messages from a Kafka Cluster")
public class ConsumerController {

	private final static Logger LOGGER = LoggerFactory.getLogger(ConsumerController.class);

	private ApplicationContext applicationContext;

	public ConsumerController(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Value("${consumer.poolTimeout}")
	private Long poolTimeout;

	@Operation(summary = "Get a list of records from a topic", tags = { "consumer" })
	@ApiResponses(value = {
		@ApiResponse(
				responseCode = "200",
				description = "List of records from topic",
				content = @Content(schema = @Schema(implementation = MessageListDTO.class))),
		@ApiResponse(responseCode = "404", description = "Not records in topic"),
		@ApiResponse(responseCode = "500", description = "Internal Server Error") })
	@GetMapping(value = "/kafka/{topicName}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<MessageListDTO> pollFromTopic(
			@Parameter(description = "Topic name", required = true) @PathVariable String topicName,
			@Parameter(description = "Commit results", required = false) @RequestParam(defaultValue = "true") boolean commit,
			@Parameter(description = "Partition ID", required = false) @RequestParam(required = false) Integer partition) {

		int messageFound = 0;

		// API Response
		MessageListDTO response = new MessageListDTO();
		List<MessageDTO> messageDTOS = new ArrayList<>();

		@SuppressWarnings("unchecked")
		Consumer<String, GenericRecord> consumer = applicationContext.getBean(Consumer.class);

		// TODO Refactor to a KafkaFacade class

		try {
			// Assign to partition defined
			if (null != partition) {
				TopicPartition topicPartition = new TopicPartition(topicName, partition);
				consumer.assign(Collections.singletonList(topicPartition));

				LOGGER.info("Consumer assigned to topic {} and partition {}", topicName, partition);
			} else {
				// Subscribe to Topic
				consumer.subscribe(Collections.singletonList(topicName));

				LOGGER.info("Consumer subscribed to topic {}", topicName);
			}

			LOGGER.info("Polling records from topic {}", topicName);

			ConsumerRecords<String, GenericRecord> consumerRecords = consumer.poll(Duration.ofSeconds(poolTimeout));

			LOGGER.info("Polled #{} records from topic {}", consumerRecords.count(), topicName);

			consumerRecords.forEach(record -> {
				MessageDTO messageDTO = new MessageDTO();
				// TODO Create a Mapper
				messageDTO.setTimestamp((Long) record.value().get("timestamp"));
				messageDTO.setContent(record.value().get("content").toString());
				// Record Metadata
				messageDTO.setKey((null != record.key() ? record.key().toString() : null));
				messageDTO.setPartition(record.partition());
				messageDTO.setOffset(record.offset());
				messageDTO.setTimestamp(record.timestamp());

				messageDTOS.add(messageDTO);
			});

			// Commit consumption
			if (Boolean.valueOf(commit)) {
				consumer.commitAsync();

				LOGGER.info("Records committed in topic {} from consumer", topicName);
			}
		} finally {
			consumer.close();
		}

		// Prepare response
		if (messageDTOS.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
		}

		response.setMessages(messageDTOS);

		return ResponseEntity.ok(response);
	}

}

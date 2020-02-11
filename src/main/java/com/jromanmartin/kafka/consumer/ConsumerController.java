package com.jromanmartin.kafka.consumer;

import com.jromanmartin.kafka.model.CustomMessage;
import com.jromanmartin.kafka.model.CustomMessageList;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

	@Autowired
	private ApplicationContext applicationContext;

	@Value("${consumer.poolTimeout:10}")
	private Long poolTimeout;

	@Operation(summary = "Get a list of records from a topic", tags = { "consumer" })
	@ApiResponses(value = {
		@ApiResponse(
				responseCode = "200",
				description = "List of records from topic",
				content = @Content(schema = @Schema(implementation = CustomMessageList.class))),
		@ApiResponse(responseCode = "404", description = "Not records in topic"),
		@ApiResponse(responseCode = "500", description = "Internal Server Error") })
	@GetMapping(value = "/kafka/{topicName}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<CustomMessageList> pollFromTopic(
			@Parameter(description = "Topic name", required = true) @PathVariable String topicName,
			@Parameter(description = "Commit results", required = false) @RequestParam(defaultValue = "true") boolean commit,
			@Parameter(description = "Partition ID", required = false) @RequestParam(required = false) Integer partition) {
		int messageFound = 0;
		List<ConsumerRecord<Long, CustomMessage>> records = new ArrayList<>();
		CustomMessageList response = new CustomMessageList();

		@SuppressWarnings("unchecked")
		Consumer<Long, CustomMessage> consumer = applicationContext.getBean(Consumer.class);

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

			ConsumerRecords<Long, CustomMessage> consumerRecords = consumer.poll(Duration.ofSeconds(poolTimeout));

			LOGGER.info("Polled #{} records from topic {}", consumerRecords.count(), topicName);

			consumerRecords.forEach(record -> {
				// Record Metadata
				record.value().setOffset(record.offset());
				record.value().setPartition(record.partition());

				records.add(record);
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
		if (records.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
		}

		records.forEach(record -> {
			response.addCustomMessage(record.value());
		});

		return ResponseEntity.ok(response);
	}

}

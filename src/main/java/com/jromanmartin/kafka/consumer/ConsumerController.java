package com.jromanmartin.kafka.consumer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.jromanmartin.kafka.model.CustomMessage;
import com.jromanmartin.kafka.model.CustomMessageList;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Controller
@RequestMapping("/consumer")
@Api(value = "/consumer", description = "Operations to consume messages from a Kafka Cluster")
public class ConsumerController {

	private final static Logger LOGGER = LoggerFactory.getLogger(ConsumerController.class);

	@Autowired
	private ApplicationContext applicationContext;

	@Value("${consumer.poolTimeout:10}")
	private Long poolTimeout;

	@ApiOperation(value = "Get a list of records from a topic", response = CustomMessageList.class)
	@ApiResponses(value = { 
		@ApiResponse(code = 200, message = "List of records from topic", response = CustomMessageList.class),
		@ApiResponse(code = 404, message = "Not records in topic"),
		@ApiResponse(code = 500, message = "Internal Server Error") })
	@GetMapping(value = "/kafka/{topicName}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<CustomMessageList> pollFromTopic(
			@ApiParam(name = "topicName", value = "Topic Name") @PathVariable String topicName, 
			@ApiParam(name = "commit", value = "Commit messages consumed") @RequestParam(defaultValue = "true") boolean commit,
			@ApiParam(name = "partition", value = "Topic Partition number", example = "0") @RequestParam(required = false) Integer partition) {
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

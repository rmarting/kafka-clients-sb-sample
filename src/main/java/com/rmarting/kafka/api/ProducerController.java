package com.rmarting.kafka.api;

import com.rmarting.kafka.dto.MessageDTO;
import com.rmarting.kafka.service.MessageService;
// OpenAPI provided by Microprofile
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.Parameter;
//import io.swagger.v3.oas.annotations.media.Content;
//import io.swagger.v3.oas.annotations.media.Schema;
//import io.swagger.v3.oas.annotations.responses.ApiResponse;
//import io.swagger.v3.oas.annotations.responses.ApiResponses;
//import io.swagger.v3.oas.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/producer")
@Tag(name = "producer", description = "Operations to produce messages to a Kafka Cluster")
@Scope("prototype")
public class ProducerController {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ProducerController.class);

	@Autowired
	MessageService messageService;

	@Operation(summary = "Send a message synchronously using the Kafka Client Producer API"/*, tags = { "producer"}*/)
    @APIResponses(value = {
		@APIResponse(
				responseCode = "200",
				description = "Message sent",
				content = @Content(schema = @Schema(implementation = MessageDTO.class))),
		@APIResponse(responseCode = "404", description = "Message not sent"),
   		@APIResponse(responseCode = "500", description = "Internal Server Error")
	})
	@PostMapping(value = "/kafka/{topicName}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<MessageDTO> sendToTopic(
			@Parameter(description = "Topic name", required = true) @PathVariable String topicName,
			@Parameter(description = "Message to send", required = true) @RequestBody MessageDTO messageDTO) {
		messageDTO = messageService.publishSync(topicName, messageDTO);

		LOGGER.debug("Published successfully message (200) into topic {}", topicName);

		return ResponseEntity.ok(messageDTO);
	}

	@Operation(summary = "Send a message asynchronously using the Kafka Client Producer API"/*, tags = { "producer"}*/)
	@APIResponses(value = {
			@APIResponse(
					responseCode = "200",
					description = "Message sent",
					content = @Content(schema = @Schema(implementation = MessageDTO.class))),
			@APIResponse(responseCode = "404", description = "Message not sent"),
			@APIResponse(responseCode = "500", description = "Internal Server Error")
	})
	@PostMapping(value = "/kafka/async/{topicName}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<MessageDTO> sendToTopicAsync(
			@Parameter(description = "Topic name", required = true) @PathVariable String topicName,
			@Parameter(description = "Topic name", required = true) @RequestBody MessageDTO messageDTO) {
		messageDTO = messageService.publishAsync(topicName, messageDTO);

		LOGGER.debug("Published successfully async message (200) into topic {}", topicName);

		return ResponseEntity.ok(messageDTO);
	}

	@Operation(summary = "Send a message synchronously using the Spring Kafka KafkaTemplate API"/*, tags = { "producer"}*/)
	@APIResponses(value = {
			@APIResponse(
					responseCode = "200",
					description = "Message sent",
					content = @Content(schema = @Schema(implementation = MessageDTO.class))),
			@APIResponse(responseCode = "404", description = "Message not sent"),
			@APIResponse(responseCode = "500", description = "Internal Server Error")
	})
	@PostMapping(value = "/spring/{topicName}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<MessageDTO> sendToTopicBySpring(
			@Parameter(description = "Topic name", required = true) @PathVariable String topicName,
			@Parameter(description = "Message to send", required = true) @RequestBody MessageDTO messageDTO) {
		messageDTO = messageService.sendMessage(topicName, messageDTO);

		LOGGER.debug("Sent successfully message (200) into topic {}", topicName);

		return ResponseEntity.ok(messageDTO);
	}

}

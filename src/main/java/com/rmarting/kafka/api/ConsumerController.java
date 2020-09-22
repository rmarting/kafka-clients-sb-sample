package com.rmarting.kafka.api;

import com.rmarting.kafka.dto.MessageListDTO;
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
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/consumer")
@Tag(name = "consumer", description = "Operations to consume messages from a Kafka Cluster")
@Scope("prototype")
public class ConsumerController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerController.class);

    @Autowired
    MessageService messageService;

    @Value("${app.consumer.poolTimeout}")
    Long poolTimeout;

    @Operation(summary = "Get a list of records from a topic"/*, tags = {"consumer"}*/)
    @APIResponses(value = {
            @APIResponse(
                    responseCode = "200",
                    description = "List of records from topic",
                    content = @Content(schema = @Schema(implementation = MessageListDTO.class))),
            @APIResponse(responseCode = "404", description = "Not records in topic"),
            @APIResponse(responseCode = "500", description = "Internal Server Error")})
    @GetMapping(value = "/kafka/{topicName}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MessageListDTO> pollFromTopic(
            @Parameter(description = "Topic name", required = true) @PathVariable String topicName,
            @Parameter(description = "Commit results", required = false) @RequestParam(defaultValue = "true") boolean commit,
            @Parameter(description = "Partition ID", required = false) @RequestParam(required = false) Integer partition) {
        MessageListDTO messageListDTO = messageService.pollEvents(topicName, partition, commit);

        // Prepare response
        if (messageListDTO.getMessages().isEmpty()) {
            LOGGER.debug("Not found messages (404) in topic {}", topicName);

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(messageListDTO);
        }

        LOGGER.debug("Pulled successfully messages (200) from topic {}", topicName);

        return ResponseEntity.ok(messageListDTO);
    }

}

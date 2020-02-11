package com.jromanmartin.kafka.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public GroupedOpenApi producerOpenApi() {
        String[] paths = { "/producer/**" };
        return GroupedOpenApi.builder()
                .setGroup("producer")
                .packagesToScan("com.jromanmartin.kafka.producer")
                .pathsToMatch(paths).build();
    }

    @Bean
    public GroupedOpenApi consumerOpenApi() {
        String[] paths = { "/consumer/**" };
        return GroupedOpenApi.builder()
                .setGroup("consumer")
                .pathsToMatch(paths)
                .packagesToScan("com.jromanmartin.kafka.consumer")
                .build();
    }

    @Bean
    public OpenAPI customOpenAPI(@Value("${springdoc.version}") String appVersion) {
        return new OpenAPI()
                .components(new Components())
                .info(new Info()
                        .title("Kafka Client Application API")
                        .version(appVersion)
                        .license(new License().name("Apache 2.0").url("http://springdoc.org"))
                        .description("Sample Spring Boot RESTful service using springdoc-openapi and OpenAPI 3 to produce and consume messages from a Kafka Cluster"))
                .addTagsItem(new Tag().name("producer")).addTagsItem(new Tag().name("consumer"));
    }

}

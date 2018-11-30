package com.jromanmartin.kafka.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

	@Bean
	public Docket producerApi() {
		return new Docket(DocumentationType.SWAGGER_2)
			.groupName("producer")
			.apiInfo(producerApiInfo())
			.select()
			.apis(RequestHandlerSelectors.basePackage("com.jromanmartin.kafka.producer"))
			.paths(producerPaths())
			.build();
	}

	@Bean
	public Docket consumerApi() {
		return new Docket(DocumentationType.SWAGGER_2)
			.groupName("consumer")
			.apiInfo(consumerApiInfo())
			.select()
			.apis(RequestHandlerSelectors.basePackage("com.jromanmartin.kafka.consumer"))
			.paths(consumerPaths())
			.build();
	}

	/**
	 * Description of Producer API
	 * 
	 * @return Api information
	 */
	private ApiInfo producerApiInfo() {
		return new ApiInfoBuilder()
				.title("Producer Kafka Client REST APIs")
				.description("This page lists all the rest apis for Producer Kafka Clients App.")
				.version("0.0.1-SNAPSHOT")
				.build();
	}

	/**
	 * Select apis that matches with producer Predicates
	 * @return List of endpoints
	 */
	private Predicate<String> producerPaths() {
		// Match all paths except /error
		// return Predicates.and(PathSelectors.regex("/consumer.*"), PathSelectors.regex("/consumer.*"), PathSelectors.regex("/producer.*"), Predicates.not(PathSelectors.regex("/error.*")));
		return Predicates.and(PathSelectors.regex("/producer.*"), Predicates.not(PathSelectors.regex("/error.*")));
	}

	/**
	 * Description of Consumer API
	 * 
	 * @return Api information
	 */
	private ApiInfo consumerApiInfo() {
		return new ApiInfoBuilder()
				.title("Consumer Kafka Client REST APIs")
				.description("This page lists all the rest apis for Consumer Kafka Clients App.")
				.version("0.0.1-SNAPSHOT")
				.build();
	}

	/**
	 * Select apis that matches with producer Predicates
	 * @return List of endpoints
	 */
	private Predicate<String> consumerPaths() {
		// Match all paths except /error
		return Predicates.and(PathSelectors.regex("/consumer.*"), Predicates.not(PathSelectors.regex("/error.*")));
	}

}
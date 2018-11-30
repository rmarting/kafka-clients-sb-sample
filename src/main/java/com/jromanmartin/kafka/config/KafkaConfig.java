package com.jromanmartin.kafka.config;

import java.util.Properties;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import com.jromanmartin.kafka.custom.CustomDeserializer;
import com.jromanmartin.kafka.custom.CustomPartitioner;
import com.jromanmartin.kafka.custom.CustomSerializer;

@Configuration
public class KafkaConfig implements KafkaConstants {
	
	@Value("${kafka.bootstrap-servers}")
	private String kafkaBrokers;
	
	@Value("${kafka.clientId}")
	private String clientId;

	@Value("${kafka.groupId}")
	private String groupId;

	@Bean
	@Scope(scopeName = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public Producer<Long, String> createProducer() {
		Properties props = new Properties();
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers);
		props.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
		
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class.getName());
		//props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, CustomSerializer.class.getName());
		
		props.put(ProducerConfig.PARTITIONER_CLASS_CONFIG, CustomPartitioner.class.getName());
		
		return new KafkaProducer<>(props);
	}
	
	@Bean
	@Scope(scopeName = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public Consumer<Long, String> createConsumer() {
		Properties props = new Properties();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers);
		props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
		
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class.getName());
		// props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, CustomDeserializer.class.getName());
		
		props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, KafkaConstants.MAX_POLL_RECORDS);
		props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, KafkaConstants.OFFSET_RESET_EARLIER);

		Consumer<Long, String> consumer = new KafkaConsumer<>(props);
				
		return consumer;
	}
	
}
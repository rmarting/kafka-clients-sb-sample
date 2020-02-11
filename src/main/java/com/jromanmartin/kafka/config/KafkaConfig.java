package com.jromanmartin.kafka.config;

import com.jromanmartin.kafka.custom.CustomDeserializer;
import com.jromanmartin.kafka.custom.CustomSerializer;
import com.jromanmartin.kafka.model.CustomMessage;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Properties;

@Configuration
public class KafkaConfig {

	public static Integer MESSAGE_COUNT=1000;

	public static String OFFSET_RESET_LATEST="latest";

	public static String OFFSET_RESET_EARLIER="earliest";

	public static Integer MAX_POLL_RECORDS=1;
	
	@Value("${kafka.bootstrap-servers}")
	private String kafkaBrokers;

	@Value("${producer.acks:1}")
	private String acks;

	@Value("${kafka.groupId}")
	private String groupId;

	@Value("${kafka.clientId}")
	private String clientId;

	@Value("${consumer.maxPoolRecords:1000}")
	private String maxPoolRecords;

	@Value("${consumer.offsetReset:earliest}")
	private String offsetReset;

	private String getHostname() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			return "UnknownHost";
		}
	}

	@Bean
	@Scope(scopeName = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public Producer<Long, String> createProducer() {
		Properties props = new Properties();

		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers);

		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class.getName());
		//props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, CustomSerializer.class.getName());

		// Allow to define a custom partitioner class
		// props.put(ProducerConfig.PARTITIONER_CLASS_CONFIG, CustomPartitioner.class.getName());

		// Acknowledgement
		props.put(ProducerConfig.ACKS_CONFIG, acks);

		return new KafkaProducer<>(props);
	}
	
	@Bean
	@Scope(scopeName = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public Consumer<Long, String> createConsumer() {
		Properties props = new Properties();

		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers);

		/*
		 * With group id, kafka broker ensures that the same message is not consumed more then once by a
		 * consumer group meaning a message can be only consumed by any one member a consumer group.
		 *
		 * Consumer groups is also a way of supporting parallel consumption of the data i.e. different consumers of
		 * the same consumer group consume data in parallel from different partitions.
		 */
		props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

		/*
		 * In addition to group.id, each consumer also identifies itself to the Kafka broker using consumer.id.
		 * This is used by Kafka to identify the currently ACTIVE consumers of a particular consumer group.
		 */
		props.put(ConsumerConfig.CLIENT_ID_CONFIG, clientId + getHostname());

		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class.getName());
		// props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, CustomDeserializer.class.getName());

		// Pool size
		props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPoolRecords);

		/*
		 * If true the consumer's offset will be periodically committed in the background.
		 * Disabled to allow commit or not under some circunstances
		 */
		props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

		/*
		 * What to do when there is no initial offset in Kafka or if the current offset does not exist any more on the
		 * server:
		 *   earliest: automatically reset the offset to the earliest offset
		 *   latest: automatically reset the offset to the latest offset
		 */
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, offsetReset);

		Consumer<Long, String> consumer = new KafkaConsumer<>(props);
				
		return consumer;
	}

	@Bean
	public ConsumerFactory<String, CustomMessage> consumerFactory(KafkaProperties kafkaProperties) {
		Map<String, Object> consumerProperties = kafkaProperties.buildConsumerProperties();

		return new DefaultKafkaConsumerFactory<>(
				consumerProperties,
				new StringDeserializer(),
				new JsonDeserializer<>(CustomMessage.class));
	}

	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, CustomMessage> kafkaListenerContainerFactory(KafkaProperties kafkaProperties) {
		ConcurrentKafkaListenerContainerFactory<String, CustomMessage> factory = new ConcurrentKafkaListenerContainerFactory<>();

		factory.setConsumerFactory(consumerFactory(kafkaProperties));
		// Enable batch processing in listeners
		// factory.setBatchListener(true);

		return factory;
	}

}
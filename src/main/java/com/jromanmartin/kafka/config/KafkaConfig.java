package com.jromanmartin.kafka.config;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.apache.avro.generic.GenericRecord;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.subject.RecordNameStrategy;
import io.confluent.kafka.serializers.subject.TopicRecordNameStrategy;

@EnableKafka
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

	@Value("${registry.url}")
	private String registryUrl;

	@Autowired
	private KafkaProperties kafkaProperties;

	private String getHostname() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			return "UnknownHost";
		}
	}


	@Bean
	public Map<String, Object> producerConfigs() {
		Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties());
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers);

		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);

		// Allow to define a custom partitioner class
		// props.put(ProducerConfig.PARTITIONER_CLASS_CONFIG, CustomPartitioner.class.getName());

		// Acknowledgement
		props.put(ProducerConfig.ACKS_CONFIG, acks);

		// Schema registry
		props.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, registryUrl);
		props.put(AbstractKafkaAvroSerDeConfig.AUTO_REGISTER_SCHEMAS, true); // <--------------
		props.put(AbstractKafkaAvroSerDeConfig.VALUE_SUBJECT_NAME_STRATEGY, RecordNameStrategy.class);

		return props;
	}

	@Bean
	public ProducerFactory<String, SpecificRecord> producerFactory() {
			return new DefaultKafkaProducerFactory<>(producerConfigs());
	}

	@Bean
	public Producer<String, SpecificRecord> createProducer() {
		return producerFactory().createProducer();
	}

	@Bean
	public KafkaTemplate<String, SpecificRecord> kafkaTemplate() {
			return new KafkaTemplate<>(producerFactory());
	}

	@Bean
	public Map<String, Object> consumerConfigs() {
		Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties());

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

		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);

		// Pool size
		props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPoolRecords);

		/*
		 * If true the consumer's offset will be periodically committed in the background.
		 * Disabled to allow commit or not under some circumstances
		 */
		props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

		/*
		 * What to do when there is no initial offset in Kafka or if the current offset does not exist any more on the
		 * server:
		 *   earliest: automatically reset the offset to the earliest offset
		 *   latest: automatically reset the offset to the latest offset
		 */
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, offsetReset);

		// Schema registry
		props.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, registryUrl);
		props.put(AbstractKafkaAvroSerDeConfig.AUTO_REGISTER_SCHEMAS, true);
		props.put(AbstractKafkaAvroSerDeConfig.VALUE_SUBJECT_NAME_STRATEGY, TopicRecordNameStrategy.class);
		props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
		// props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, false);

		return props;
	}

	@Bean
	public ConsumerFactory<String, GenericRecord> consumerFactory() {
		return new DefaultKafkaConsumerFactory<>(consumerConfigs());
	}

	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, GenericRecord> kafkaListenerContainerFactory() {
		ConcurrentKafkaListenerContainerFactory<String, GenericRecord> factory = new ConcurrentKafkaListenerContainerFactory<>();

		factory.setConsumerFactory(consumerFactory());
		// Enable batch processing in listeners
		// factory.setBatchListener(true);

		return factory;
	}

	@Bean
	public Consumer<String, GenericRecord> createConsumer() {
		return consumerFactory().createConsumer();
	}

}
package com.rmarting.kafka.config;

import com.rmarting.kafka.schema.avro.Message;
import io.apicurio.registry.utils.serde.AbstractKafkaSerDe;
import io.apicurio.registry.utils.serde.AbstractKafkaSerializer;
import io.apicurio.registry.utils.serde.AvroKafkaDeserializer;
import io.apicurio.registry.utils.serde.AvroKafkaSerializer;
import io.apicurio.registry.utils.serde.avro.AvroDatumProvider;
import io.apicurio.registry.utils.serde.strategy.TopicIdStrategy;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Properties;

@Configuration
public class KafkaConfig {

    @Value("${kafka.bootstrap-servers:localhost:8080}")
    private String kafkaBrokers;

    @Value("${kafka.user.name}")
    private String kafkaUser;

    @Value("${kafka.user.password}")
    private String kafkaPassword;

    @Value("${kafka.security.protocol}")
    private String kafkaSecurityProtocol;

    @Value("${producer.clienId:kafka-client-sb-producer-client}")
    private String producerClientId;

    @Value("${producer.acks:1}")
    private String acks;

    @Value("${consumer.groupId:kafka-client-sb-consumer}")
    private String consumerGroupId;

    @Value("${consumer.clientId:kafka-client-sb-consumer-client}")
    private String consumerClientId;

    @Value("${consumer.maxPoolRecords:1000}")
    private String maxPoolRecords;

    @Value("${consumer.offsetReset:earliest}")
    private String offsetReset;

    @Value("${consumer.autoCommit:false}")
    private String autoCommit;

    @Value("${apicurio.registry.url:http://localhost:8080/api}")
    private String serviceRegistryUrl;

    private String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "UnknownHost";
        }
    }

    @Bean
    @Scope(scopeName = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public Producer<String, Message> createProducer() {
        Properties props = new Properties();

        // Kafka Bootstrap
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers);

        // Security
        props.put(AdminClientConfig.SECURITY_PROTOCOL_CONFIG, kafkaSecurityProtocol);
        props.put(SaslConfigs.SASL_MECHANISM, "SCRAM-SHA-512");
        props.put(SaslConfigs.SASL_JAAS_CONFIG,
                "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"" + kafkaUser
                        + "\" password=\"" + kafkaPassword + "\";");

        // Producer Client
        props.putIfAbsent(ProducerConfig.CLIENT_ID_CONFIG, producerClientId + "-" + getHostname());

        // Serializers for Keys and Values
        props.putIfAbsent(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.putIfAbsent(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, AvroKafkaSerializer.class.getName());

        // Service Registry
        props.putIfAbsent(AbstractKafkaSerDe.REGISTRY_URL_CONFIG_PARAM, serviceRegistryUrl);
        // Artifact Id Strategies (implementations of ArtifactIdStrategy)
        // Simple Topic Id Strategy (schema = topicName)
        //props.putIfAbsent(AbstractKafkaSerializer.REGISTRY_ARTIFACT_ID_STRATEGY_CONFIG_PARAM, SimpleTopicIdStrategy.class.getName());
        // Topic Id Strategy (schema = topicName-(key|value)) - Default Strategy
        props.putIfAbsent(AbstractKafkaSerializer.REGISTRY_ARTIFACT_ID_STRATEGY_CONFIG_PARAM, TopicIdStrategy.class.getName());
        // Record Id Strategy (schema = full name of the schema (namespace.name))
        //props.putIfAbsent(AbstractKafkaSerializer.REGISTRY_ARTIFACT_ID_STRATEGY_CONFIG_PARAM, RecordIdStrategy.class.getName());
        // Topic Record Id Strategy (schema = topic name and the full name of the schema (topicName-namespace.name)
        //props.putIfAbsent(AbstractKafkaSerializer.REGISTRY_ARTIFACT_ID_STRATEGY_CONFIG_PARAM, TopicRecordIdStrategy.class.getName());

        // Global Id Strategies (implementations of GlobalIdStrategy)
        //props.putIfAbsent(AbstractKafkaSerializer.REGISTRY_GLOBAL_ID_STRATEGY_CONFIG_PARAM, FindLatestIdStrategy.class.getName());
        //props.putIfAbsent(AbstractKafkaSerializer.REGISTRY_GLOBAL_ID_STRATEGY_CONFIG_PARAM, FindBySchemaIdStrategy.class.getName());
        //props.putIfAbsent(AbstractKafkaSerializer.REGISTRY_GLOBAL_ID_STRATEGY_CONFIG_PARAM, GetOrCreateIdStrategy.class.getName());
        //props.putIfAbsent(AbstractKafkaSerializer.REGISTRY_GLOBAL_ID_STRATEGY_CONFIG_PARAM, AutoRegisterIdStrategy.class.getName());

        // Acknowledgement
        props.putIfAbsent(ProducerConfig.ACKS_CONFIG, acks);

        return new KafkaProducer<>(props);
    }

    @Bean
    @Scope(scopeName = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public Consumer<String, Message> createConsumer() {
        Properties props = new Properties();

        // Kafka Bootstrap
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers);

        // Security
        props.put(AdminClientConfig.SECURITY_PROTOCOL_CONFIG, kafkaSecurityProtocol);
        props.put(SaslConfigs.SASL_MECHANISM, "SCRAM-SHA-512");
        props.put(SaslConfigs.SASL_JAAS_CONFIG,
                "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"" + kafkaUser
                        + "\" password=\"" + kafkaPassword + "\";");

        /*
         * With group id, kafka broker ensures that the same message is not consumed more then once by a
         * consumer group meaning a message can be only consumed by any one member a consumer group.
         *
         * Consumer groups is also a way of supporting parallel consumption of the data i.e. different consumers of
         * the same consumer group consume data in parallel from different partitions.
         */
        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);

        /*
         * In addition to group.id, each consumer also identifies itself to the Kafka broker using consumer.id.
         * This is used by Kafka to identify the currently ACTIVE consumers of a particular consumer group.
         */
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, consumerClientId + "-" + getHostname());

        // Deserializers for Keys and Values
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, AvroKafkaDeserializer.class.getName());

        // Pool size
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPoolRecords);

        /*
         * If true the consumer's offset will be periodically committed in the background.
         * Disabled to allow commit or not under some circumstances
         */
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, autoCommit);

        /*
         * What to do when there is no initial offset in Kafka or if the current offset does not exist any more on the
         * server:
         *   earliest: automatically reset the offset to the earliest offset
         *   latest: automatically reset the offset to the latest offset
         */
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, offsetReset);

        // Service Registry Integration
        props.put(AbstractKafkaSerDe.REGISTRY_URL_CONFIG_PARAM, serviceRegistryUrl);
        // Use Specific Avro classes instead of the GenericRecord class definition
        props.put(AvroDatumProvider.REGISTRY_USE_SPECIFIC_AVRO_READER_CONFIG_PARAM, true);

        return new KafkaConsumer<>(props);
    }

    @Bean
    public ProducerFactory<String, Message> producerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> configProps = kafkaProperties.buildProducerProperties();

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Message> kafkaTemplate(KafkaProperties kafkaProperties) {
        return new KafkaTemplate<>(producerFactory(kafkaProperties));
    }

}

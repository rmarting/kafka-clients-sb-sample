# Kafka Clients on Spring Boot Sample

An example project about Kafka Clients on Spring Boot.

# Build and Run

Before to build the application it is needed to set up some values in **src/main/resources/application.properties** file.

Review and set up the right values from your Kafka Cluster 

* Kafka Boostrap Servers: 

    kafka.bootstrap-servers = localhost:9092

* Client and Group Id:

    kafka.clientId = kafkaClientSbSample
    kafka.groupId  = kafkaConsumerGroup01

To build the application:

    mvn clean package

To run the application:

     java -jar target/kafka-clients-sb-sample.jar

# REST API

REST API is available from a Swagger UI at:

    http://localhost:8080/swagger-ui.html

There is two groups to manage a topic from a Kafka Cluster.

* Producer: Send messages to a topic 
* Consumer: Consume messages from a topic

## Producer REST API

Sample REST API to send messages to a Kafka Topic.

    http://localhost:8080/swagger-ui.html#/producer-controller

The most common parameters for some operations:

* topicName: Topic Name
* message: Message content based in a custom message:

Model:

    CustomMessage {
        key (integer, optional): Key to identify this message ,
        timestamp (string, optional, read only): Timestamp ,
        content (string): Content ,
        partition (integer, optional, read only): Partition number ,
        offset (integer, optional, read only): Offset in the partition
    }

Simple Sample:

    $ curl -X POST http://localhost:8080/producer/kafka/my-topic -H "Content-Type:application/json" -d '{"key": "1", "content": "Simple message"}'
    {"key":1,"timestamp":"30/45/2018 10:45:03","content":"Simple message","partition":1,"offset":4}

## Consumer REST API

Sample REST API to consume messages from a Kafka Topic.

    http://localhost:8080/swagger-ui.html#/consumer-controller


The most common parameters for some operations:

* topicName: Topic Name (Required)
* size: Number of messages to consume (Required)
* commit: Commit messaged consumed. Values: true|false
* partition: Number of the partition to consume (Optional)

Simple Sample:

    $ curl -v "http://localhost:8080/consumer/kafka/topic-partitionated?size=4&commit=true&partition=1" | jq
    {                
        "customMessages": [
            {
                "key": 1,
                "timestamp": "30/49/2018 10:49:36",
                "content": "Simple message",
                "partition": 0,
                "offset": 0
            },
            {
                "key": 1,
                "timestamp": "30/49/2018 10:49:39",
                "content": "Simple message",
                "partition": 0,
                "offset": 0
            },
            {
                "key": 1,
                "timestamp": "30/49/2018 10:49:41",
                "content": "Simple message",
                "partition": 0,
                "offset": 0
            }
        ]
    }


# Kafka Cluster

Kafka Cluster is available a enterprise distribution from Red Hat: [Red Hat AMQ Streams](https://www.redhat.com/en/blog/announcing-red-hat-amq-streams-apache-kafka-red-hat-openshift).

A Red Hat Developer subscription helps you to download a distribution and deploy in your local environment.

From a local installation to start a single cluster: 

    $ ./bin/zookeeper-server-start.sh -daemon ./config/zookeeper.properties 
    $ ./bin/kafka-server-start.sh -daemon ./config/server.properties 

To stop the single cluster

    $ ./bin/kafka-server-stop.sh 
    $ ./bin/zookeeper-server-stop.sh 

To deploy a Kafka Topic:

    $ ./bin/kafka-topics.sh --zookeeper localhost:2181 --create --topic my-topic --partitions 6 --replication-factor 1

# References

* [Using AMQ Streams on RHEL](https://access.redhat.com/documentation/en-us/red_hat_amq/7.2/html-single/using_amq_streams_on_red_hat_enterprise_linux_rhel/)
* [Kafka Producer and Consumer Examples Using Java](https://dzone.com/articles/kafka-producer-and-consumer-example)
* [Kafka Tutorial: Writing a Kafka Producer in Java](http://cloudurable.com/blog/kafka-tutorial-kafka-producer/index.html)
* [Writing a Kafka Consumer in Java](https://dzone.com/articles/writing-a-kafka-consumer-in-java)


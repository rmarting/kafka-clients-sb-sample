# Kafka Clients on Spring Boot Sample

This sample project demonstrates how to use Kafka Clients on Spring Boot to send and consume messages from an
[Apache Kafka](https://kafka.apache.org/) cluster managed by [Strimzi](https://strimzi.io/) operators deployed on a 
Kubernetes and OpenShift Platform.

The example includes a simple REST API with the following operations:

* Send messages to a Topic
* Consume messages from a Topic

This project only uses [Kafka Producer API](https://kafka.apache.org/documentation/#producerapi) and 
[Kafka Consumer API](https://kafka.apache.org/documentation/#consumerapi).

## Environment

This project requires a Kubernetes and OpenShift platform available. If you do not have one, you could use 
one of the following resources to deploy locally a Kubernetes and OpenShift Cluster:

* [minishift - run OpenShift locally](https://github.com/minishift/minishift)
* [Red Hat Container Development Kit](https://developers.redhat.com/products/cdk/overview/)  
* [minikube - Running Kubernetes Locally](https://kubernetes.io/docs/setup/minikube/)

### Deploying Kafka

Strimzi includes a set of Kubernetes Operators to deploy Apache Kafka Clusters on Kubernetes and OpenShift platform.

You can follow the instructions from [Community Documentation](https://strimzi.io/docs/latest/#downloads-str) or you
could use my [Ansible Playbook](https://github.com/rmarting/strimzi-ansible-playbook) to do it. In both cases it is 
very easy to do it.

```src/main/strimzi``` folder includes a set of custom resource definitions to deploy a Kafka Cluster
and a Kafka Topic using the Strimzi Operators.

To deploy the Kafka Cluster:

```
$ kubectl apply -f src/main/strimz/kafka.yml -n <NAMESPACE>
kafka.kafka.strimzi.io/my-kafka created
```

To deploy the Kafka Topic:

```
$ kubectl apply -f src/main/strimz/kafkatopic.yml -n <NAMESPACE>
kafkatopic.kafka.strimzi.io/my-topic created
```

After some minutes Kafka Cluster will be deployed:

```
$ kubectl get pod
NAME                                           READY     STATUS      RESTARTS   AGE
my-kafka-entity-operator-85cd57f94d-x6h2w      3/3       Running     0          1m
my-kafka-kafka-0                               2/2       Running     1          3m
my-kafka-kafka-1                               2/2       Running     0          3m
my-kafka-kafka-2                               2/2       Running     0          3m
my-kafka-zookeeper-0                           2/2       Running     0          4m
my-kafka-zookeeper-1                           2/2       Running     0          4m
my-kafka-zookeeper-2                           2/2       Running     0          4m
strimzi-cluster-operator-c8d786dcb-8rt9v       1/1       Running     2          5d
```

# Build and Deploy

Before to build the application it is needed to set up some values in **src/main/resources/application.properties** file.

Review and set up the right values from your Kafka Cluster 

* Kafka Boostrap Servers: Kafka brokers are defined by a Kubernetes and OpenShift service created by Strimzi when the Kafka 
cluster is deployed. This service, called *cluster-name*-kafka-boostrap exposes 9092 port for plain traffic and 9093 
for encrypted traffic. 

```
kafka.bootstrap-servers=my-kafka-kafka-bootstrap:9092
```

To build the application:

```
$ mvn clean package
```

To deploy the application:

```
$ mvn fabric8:deploy -Popenshift
```

# REST API

REST API is available from a Swagger UI at:

```
    http://<KUBERNETES_OPENSHIFT_ROUTE_SERVICE_HOST>/swagger-ui.html
```

**KUBERNETES_OPENSHIFT_ROUTE_SERVICE_HOST** will be the route create on Kubernetes and OpenShift to expose outside the
service. For example: http://kafka-clients-sb-sample-strimzi.192.168.42.17.nip.io/swagger-ui.html

There is two groups to manage a topic from a Kafka Cluster.

* **Producer**: Send messages to a topic 
* **Consumer**: Consume messages from a topic

## Producer REST API

Sample REST API to send messages to a Kafka Topic.

```
    http://<KUBERNETES_OPENSHIFT_ROUTE_SERVICE_HOST>/swagger-ui.html#/producer-controller
```

The most common parameters for some operations:

* **topicName**: Topic Name
* **message**: Message content based in a custom message:

Model:

```
    CustomMessage {
        key (integer, optional): Key to identify this message ,
        timestamp (string, optional, read only): Timestamp ,
        content (string): Content ,
        partition (integer, optional, read only): Partition number ,
        offset (integer, optional, read only): Offset in the partition
    }
```

Simple Sample:

```
    $ curl -X POST http://<KUBERNETES_OPENSHIFT_ROUTE_SERVICE_HOST>/producer/kafka/my-topic -H "Content-Type:application/json" -d '{"content": "Simple message"}'
    {
      "key": null,
      "timestamp": 1554728931062,
      "content": "Simple message",
      "partition": 2,
      "offset": 7
    }
```

## Consumer REST API

Sample REST API to consume messages from a Kafka Topic.

```
    http://<KUBERNETES_OPENSHIFT_ROUTE_SERVICE_HOST>/swagger-ui.html#/consumer-controller
```

The most common parameters for some operations:

* **topicName**: Topic Name (Required)
* **partition**: Number of the partition to consume (Optional)
* **commit**: Commit messaged consumed. Values: true|false

Simple Sample:

```
    $ curl -v "http://<KUBERNETES_OPENSHIFT_ROUTE_SERVICE_HOST>/consumer/kafka/my-topic?commit=true&partition=2" | jq
    {
      "customMessages": [
        {
          "key": null,
          "timestamp": 1554728931062,
          "content": "Simple message",
          "partition": 2,
          "offset": 7
        }
      ]
    }
```

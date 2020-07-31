# Kafka Clients on Spring Boot Sample

This sample project demonstrates how to use Kafka Clients on Spring Boot to send and consume messages from an
[Apache Kafka](https://kafka.apache.org/) cluster managed by [Strimzi](https://strimzi.io/) operator deployed on a 
Kubernetes or OpenShift Platform.

The example includes a simple REST API with the following operations:

* Send messages to a Topic
* Consume messages from a Topic

This project only uses [Kafka Producer API](https://kafka.apache.org/documentation/#producerapi) and 
[Kafka Consumer API](https://kafka.apache.org/documentation/#consumerapi).

## Environment

This project requires a Kubernetes or OpenShift platform available. If you do not have one, you could use 
one of the following resources to deploy locally a Kubernetes and OpenShift Cluster:

* [Red Hat Container Development Kit - deploy and run an OpenShift 3.X cluster locally](https://developers.redhat.com/products/cdk/overview/)  
* [Red Hat CodeReady Containers - OpenShift 4 on your Laptop](https://github.com/code-ready/crc)

### Deploying Kafka

Strimzi includes a set of Kubernetes Operators to deploy Apache Kafka Clusters on Kubernetes and OpenShift platform.

You can follow the instructions from [Community Documentation](https://strimzi.io/docs/operators/latest/deploying.html)
or you could use my [Ansible Playbook](https://github.com/rmarting/strimzi-ansible-playbook) to do it. In both cases
it is very easy to do it.

```src/main/strimzi``` folder includes a set of custom resource definitions to deploy a Kafka Cluster
and some Kafka Topics using the Strimzi Operators.

To deploy Kafka and the rest of the resources, we will create a new namespace in the cluster as:

In Kubernetes:

```bash
kubectl create namespace amq-streams
```

In OpenShift:

```bash
oc new-project amq-streams
```

To deploy the Kafka Cluster in Kubernetes:

```bash
$ kubectl apply -f src/main/strimzi/kafka.yml -n amq-streams
kafka.kafka.strimzi.io/my-kafka created
```

In OpenShift:

```bash
$ oc apply -f src/main/strimzi/kafka.yml -n amq-streams
kafka.kafka.strimzi.io/my-kafka created
```

To deploy the Kafka Topic in Kubernetes:

```bash
$ kubectl apply -f src/main/strimzi/kafkatopic-one-topic.yml -n amq-streams
kafkatopic.kafka.strimzi.io/one-topic created
$ kubectl apply -f src/main/strimzi/kafkatopic-another-topic.yml -n amq-streams
kafkatopic.kafka.strimzi.io/another-topic created
```

In OpenShift:

```bash
$ oc apply -f src/main/strimzi/kafkatopic-one-topic.yml -n amq-streams
kafkatopic.kafka.strimzi.io/one-topic created
$ oc apply -f src/main/strimzi/kafkatopic-another-topic.yml -n amq-streams
kafkatopic.kafka.strimzi.io/another-topic created
```

After some minutes Kafka Cluster will be deployed:

```bash
$ kubectl get pod -n amq-streams
NAME                                           READY   STATUS    RESTARTS   AGE
my-kafka-entity-operator-8474bb6769-xqzt9      3/3     Running   0          1m
my-kafka-kafka-0                               2/2     Running   0          2m
my-kafka-kafka-1                               2/2     Running   0          2m
my-kafka-kafka-2                               2/2     Running   0          2m
my-kafka-kafka-exporter-5b4dff4858-8z9gw       1/1     Running   0          30s
my-kafka-zookeeper-0                           2/2     Running   0          3m
my-kafka-zookeeper-1                           2/2     Running   0          3m
my-kafka-zookeeper-2                           2/2     Running   0          3m
strimzi-cluster-operator-c8d786dcb-8rt9v       1/1     Running   2          5d
```

# Build and Deploy

Before to build the application it is needed to set up some values in ```src/main/resources/application.properties``` file.

Review and set up the right values from your Kafka Cluster 

* Kafka Bootstrap Servers: Kafka brokers are defined by a Kubernetes and OpenShift service created by Strimzi when
the Kafka cluster is deployed. This service, called *cluster-name*-kafka-bootstrap exposes 9092 port for plain
traffic and 9093 for encrypted traffic. 

```text
kafka.bootstrap-servers=my-kafka-kafka-bootstrap=9092
```

To build the application:

```bash
$ mvn clean package
```

To run locally:

```bash
$ mvn spring-boot:run
```

Or you can deploy into Kubernetes or OpenShift platform using [Eclipse JKube](https://github.com/eclipse/jkube) Maven Plug-ins:

To deploy the application in Kubernetes:

```bash
$ mvn k8s:resource k8s:build k8s:apply -Pkubernetes -Djkube.build.strategy=jib
```

To deploy the application in OpenShift:

```bash
$ mvn oc:resource oc:build oc:apply -Popenshift
```

# REST API

REST API is available from a Swagger UI at:

```text
http://<KUBERNETES_OPENSHIFT_ROUTE_SERVICE_HOST>/swagger-ui.html
```

**KUBERNETES_OPENSHIFT_ROUTE_SERVICE_HOST** will be the route create on Kubernetes or OpenShift to expose outside the
service. 

To get the route the following command in Kubernetes give you the host:

```bash
$ kubectl get route kafka-clients-sb-sample -o jsonpath='{.spec.host}'
```

In OpenShift:

```bash
$ oc get route kafka-clients-sb-sample -o jsonpath='{.spec.host}'
```

There are two groups to manage a topic from a Kafka Cluster.

* **Producer**: Send messages to a topic 
* **Consumer**: Consume messages from a topic

## Producer REST API

Sample REST API to send messages to a Kafka Topic.

```text
http://<KUBERNETES_OPENSHIFT_ROUTE_SERVICE_HOST>/swagger-ui.html#/producer-controller
```

The most common parameters for some operations:

* **topicName**: Topic Name
* **message**: Message content based in a custom message:

Model:

```text
CustomMessage {
  key (integer, optional): Key to identify this message,
  timestamp (string, optional, read only): Timestamp,
  content (string): Content,
  partition (integer, optional, read only): Partition number,
  offset (integer, optional, read only): Offset in the partition
}
```

Simple Sample:

```bash
$ curl -X POST http://$(oc get route kafka-clients-sb-sample -o jsonpath='{.spec.host}')/producer/kafka/one-topic \
-H "Content-Type:application/json" -d '{"content": "Simple message"}' | jq
{
  "key": null,
  "timestamp": 1581087543362,
  "content": "Simple message",
  "partition": 0,
  "offset": 3
}
```

## Consumer REST API

Sample REST API to consume messages from a Kafka Topic.

```text
http://<KUBERNETES_OPENSHIFT_ROUTE_SERVICE_HOST>/swagger-ui.html#/consumer-controller
```

The most common parameters for some operations:

* **topicName**: Topic Name (Required)
* **partition**: Number of the partition to consume (Optional)
* **commit**: Commit messaged consumed. Values: true|false

Simple Sample:

```bash
$ curl -v "http://$(oc get route kafka-clients-sb-sample -o jsonpath='{.spec.host}')/consumer/kafka/one-topic?commit=true&partition=2" | jq
{
  "customMessages": [
    {
      "key": null,
      "timestamp": 1581087539350,
      "content": "Simple message",
      "partition": 2,
      "offset": 0
    },
    ...
    {
      "key": null,
      "timestamp": 1581087584266,
      "content": "Simple message",
      "partition": 2,
      "offset": 3
    }
  ]
}
```

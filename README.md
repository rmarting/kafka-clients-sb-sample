# Kafka Clients on Spring Boot Sample

This sample project demonstrates how to use [Kafka Clients](https://mvnrepository.com/artifact/org.apache.kafka/kafka-clients)
and [Spring Kafka](https://spring.io/projects/spring-kafka) on Spring Boot to send and consume message from an
[Apache Kafka](https://kafka.apache.org/) cluster managed by [Strimzi](https://strimzi.io/) operator deployed on a 
Kubernetes or OpenShift Platform. These message will be managed by an Schema Registry or Service Registry
operated by [Apicurio](https://www.apicur.io/registry/docs/apicurio-registry/index.html#intro-to-the-registry).

Apache Kafka is an open-source distributed event streaming platform for high-performance data pipelines,
streaming analytics, data integration, and mission-critical applications.

Service Registry is a datastore for sharing standard event schemas and API designs across API and event-driven architectures.
You can use Service Registry to decouple the structure of your data from your client applications, and to share and
manage your data types and API descriptions at runtime using a REST interface.

The example includes a simple REST API with the following operations:

* Send messages to a Topic
* Consume messages from a Topic

## Environment

This project requires a Kubernetes or OpenShift platform available. If you do not have one, you could use 
one of the following resources to deploy locally a Kubernetes and OpenShift Cluster:

* [Red Hat Container Development Kit - deploy and run an OpenShift 3.X cluster locally](https://developers.redhat.com/products/cdk/overview/)  
* [Red Hat CodeReady Containers - OpenShift 4 on your Laptop](https://github.com/code-ready/crc)
* [Minikube - Running Kubernetes Locally](https://kubernetes.io/docs/setup/minikube/)

> Note: in older versions of Minikube you may hit an [issue](https://github.com/kubernetes/minikube/issues/8330)
> with Persistent Volume Claims stuck in Pending status

To deploy the resources, we will create a new ```amq-streams``` namespace in the cluster as:

In Kubernetes:

```shell script
❯ kubectl create namespace amq-streams
```

In OpenShift:

```shell script
❯ oc new-project amq-streams
```

### Deploying Strimzi and Apicurio Operators

> **NOTE**: Only *cluster-admin* users could deploy Kubernetes Operators. This section must
> be executed with one of them.

To deploy Cluster Operator as cluster-wide (inspect all namespaces):

In Kubernetes:

```shell script
❯ kubectl apply -f src/main/strimzi/operator/subscription.yml 
subscription.operators.coreos.com/strimzi-kafka-operator created
```

To deploy the Apicurio Operator to manage the ```amq-streams``` namespace: 

```shell script
❯ kubectl apply -f src/main/apicurio/operator/operator-group.yml 
operatorgroup.operators.coreos.com/amq-streams-demo-og created
❯ kubectl apply -f src/main/apicurio/operator/subscription.yml 
subscription.operators.coreos.com/apicurio-registry created
```

In OpenShift:

```shell script
❯ oc apply -f src/main/strimzi/operator/subscription.yml 
subscription.operators.coreos.com/strimzi-kafka-operator created
```

To deploy the Apicurio Operator to manage the ```amq-streams``` namespace: 

In Kubernetes:

```shell script
❯ kubectl apply -f src/main/apicurio/operator/operator-group.yml 
operatorgroup.operators.coreos.com/amq-streams-demo-og created
❯ kubectl apply -f src/main/apicurio/operator/subscription.yml 
subscription.operators.coreos.com/apicurio-registry created
```

In OpenShift:

```shell script
❯ oc apply -f src/main/apicurio/operator/operator-group.yml 
operatorgroup.operators.coreos.com/amq-streams-demo-og created
❯ oc apply -f src/main/apicurio/operator/subscription.yml 
subscription.operators.coreos.com/apicurio-registry created
```

For more information about how to install Operators using the CLI command, please review this [article](
https://docs.openshift.com/container-platform/4.5/operators/olm-adding-operators-to-cluster.html#olm-installing-operator-from-operatorhub-using-cli_olm-adding-operators-to-a-cluster)

### Deploying Kafka

```src/main/strimzi``` folder includes a set of custom resource definitions to deploy a Kafka Cluster
and some Kafka Topics using the Strimzi Operators.

To deploy the Kafka Cluster in Kubernetes:

```shell script
❯ kubectl apply -f src/main/strimzi/kafka.yml -n amq-streams
kafka.kafka.strimzi.io/my-kafka created
```

To deploy the Kafka Cluster in Minikube or single node cluster:

```shell script
❯ kubectl apply -f src/main/strimzi/kafka-single.yml -n amq-streams
kafka.kafka.strimzi.io/my-kafka created
```

In OpenShift:

```shell script
❯ oc apply -f src/main/strimzi/kafka.yml -n amq-streams
kafka.kafka.strimzi.io/my-kafka created
```

To deploy the Kafka Topic in Kubernetes:

```shell script
❯ kubectl apply -f src/main/strimzi/kafkatopic-messages.yml -n amq-streams
kafkatopic.kafka.strimzi.io/messages created
```

To deploy the Kafka Topic in Minikube or single node cluster:

```shell script
❯ kubectl apply -f src/main/strimzi/kafkatopic-messages.yml -n amq-streams
kafkatopic.kafka.strimzi.io/messages created
```

In OpenShift:

```shell script
❯ oc apply -f src/main/strimzi/kafkatopic-messages.yml -n amq-streams
kafkatopic.kafka.strimzi.io/messages created
```

After some minutes Kafka Cluster will be deployed:

```shell script
❯ kubectl get pod -n amq-streams
NAME                                           READY   STATUS    RESTARTS   AGE
my-kafka-entity-operator-8474bb6769-xqzt9      3/3     Running   0          1m
my-kafka-kafka-0                               2/2     Running   0          2m
my-kafka-zookeeper-0                           2/2     Running   0          3m
```

### Service Registry

Service Registry needs a set of KafkaTopics to store schemas and metadata of them. We need to execute the following
commands to create the KafkaTopics and to deploy an instance of Service Registry:

In Kubernetes:


```shell script
❯ oc apply -f src/main/apicurio/topics/
kafkatopic.kafka.strimzi.io/global-id-topic created
kafkatopic.kafka.strimzi.io/storage-topic created
❯ oc apply -f src/main/apicurio/service-registry.yml
apicurioregistry.apicur.io/service-registry created
```

A new DeploymentConfig is created with the prefix ```service-registry-deployment-``` and a new route is
created with the prefix ```service-registry-ingres-``. We must inspect it
to get the route created to expose the Service Registry API.

In Kubernetes:

```shell script
❯ kubectl get route
NAME                                   HOST/PORT                                               PATH   SERVICES                            PORT    TERMINATION   WILDCARD
service-registry-ingress-rjmr4-8gxsd   service-registry.amq-streams.apps-crc.testing           /      service-registry-service-bfj4n      <all>                 None
```

In OpenShift:

```shell script
❯ oc get route
NAME                                   HOST/PORT                                               PATH   SERVICES                            PORT    TERMINATION   WILDCARD
service-registry-ingress-rjmr4-8gxsd   service-registry.amq-streams.apps-crc.testing           /      service-registry-service-bfj4n      <all>                 None
```

While few minutes until your Service Registry has deployed.

The Service Registry Web Console and API endpoints will be available from: 

* **Web Console**: http://<KUBERNETES_OPENSHIFT_SR_ROUTE_SERVICE_HOST>/ui/
* **API REST**: http://KUBERNETES_OPENSHIFT_SR_ROUTE_SERVICE_HOST/api/

Set up the ```apicurio.registry.url``` property in the ```pom.xml``` file the Service Registry url before to publish the
schemas used by this application:

In Kubernetes:

```shell script
❯ kubectl get route service-registry-ingress-rjmr4-8gxsd -o jsonpath='{.spec.host}'
```

In OpenShift:

```shell script
❯ oc get route service-registry-ingress-rjmr4-8gxsd -o jsonpath='{.spec.host}'
```

To register the schemas in Service Registry execute:

```shell script
❯ mvn clean generate-sources -Papicurio
```

The next screenshot shows the schemas registed in the Web Console:

![Artifacts registered in Apicurio Registry](./img/apicurio-registry-artifacts.png) 

# Build and Deploy

Before we build the application we need to set up some values in ```src/main/resources/application.properties``` file.

Review and set up the right values from your Kafka Cluster 

* **Kafka Bootstrap Servers**: Kafka brokers are defined by a Kubernetes and OpenShift service created by Strimzi when
the Kafka cluster is deployed. This service, called *cluster-name*-kafka-bootstrap exposes 9092 port for plain
traffic and 9093 for encrypted traffic. 

```text
kafka.bootstrap-servers = my-kafka-kafka-bootstrap:9092

spring.kafka.bootstrap-servers = my-kafka-kafka-bootstrap:9092
```

* **Service Registry API Endpoint**: Avro Serde classes need to communicate with the Service Registry API to check and
validate the schemas. 

```text
apicurio.registry.url = http://service-registry-service-bfj4n:8080/api
```

To build the application:

```shell script
❯ mvn clean package
```

To run locally:

```shell script
❯ mvn spring-boot:run
```

Or you can deploy into Kubernetes or OpenShift platform using [Eclipse JKube](https://github.com/eclipse/jkube) Maven Plug-ins:

To deploy the application in Kubernetes:

```shell script
❯ mvn package k8s:resource k8s:build k8s:push k8s:apply -Pkubernetes -Djkube.build.strategy=jib
```

To deploy the application in OpenShift:

```shell script
❯ mvn package oc:resource oc:build oc:apply -Popenshift
```

To deploy the application in Minikube:

```shell script
❯ eval $(minikube docker-env)
❯ mvn package k8s:resource k8s:build k8s:apply -Pkubernetes
```

# REST API

REST API is available from a Swagger UI at:

```text
http://<KUBERNETES_OPENSHIFT_ROUTE_SERVICE_HOST>/swagger-ui.html
```

**KUBERNETES_OPENSHIFT_ROUTE_SERVICE_HOST** will be the route create on Kubernetes or OpenShift to expose outside the
service. 

To get the route the following command in Kubernetes give you the host:

```shell script
❯ kubectl get route kafka-clients-sb-sample -o jsonpath='{.spec.host}'
```

In OpenShift:

```shell script
❯ oc get route kafka-clients-sb-sample -o jsonpath='{.spec.host}'
```

There are two groups to manage a topic from a Kafka Cluster.

* **Producer**: Send messageDTOS to a topic 
* **Consumer**: Consume messageDTOS from a topic

## Producer REST API

Sample REST API to send messageDTOS to a Kafka Topic.

```text
http://<KUBERNETES_OPENSHIFT_ROUTE_SERVICE_HOST>/swagger-ui.html#/producer-controller
```

The most common parameters for some operations:

* **topicName**: Topic Name
* **messageDTO**: Message content based in a custom messageDTO:

Model:

```text
MessageDTO {
  key (integer, optional): Key to identify this message,
  timestamp (string, optional, read only): Timestamp,
  content (string): Content,
  partition (integer, optional, read only): Partition number,
  offset (integer, optional, read only): Offset in the partition
}
```

Simple Sample:

```shell script
❯ curl -X POST http://$(oc get route kafka-clients-sb-sample -o jsonpath='{.spec.host}')/producer/kafka/messages \
-H "Content-Type:application/json" -d '{"content": "Simple message"}' | jq
{
  "key": null,
  "timestamp": 1581087543362,
  "content": "Simple messageDTO",
  "partition": 0,
  "offset": 3
}
```

With Minikube:

```shell script
❯ curl $(minikube ip):$(kubectl get svc kafka-clients-sb-sample -n amq-streams -o jsonpath='{.spec.ports[].nodePort}')/producer/kafka/one-topic -H "Content-Type:application/json" -d '{"content": "Simple message from Minikube"}'
{"key":null,"timestamp":1596203271368,"content":"Simple messageDTO from Minikube","partition":0,"offset":4}
```

## Consumer REST API

Sample REST API to consume messageDTOS from a Kafka Topic.

```text
http://<KUBERNETES_OPENSHIFT_ROUTE_SERVICE_HOST>/swagger-ui.html#/consumer-controller
```

The most common parameters for some operations:

* **topicName**: Topic Name (Required)
* **partition**: Number of the partition to consume (Optional)
* **commit**: Commit messaged consumed. Values: true|false

Simple Sample:

```shell script
❯ curl -v "http://$(oc get route kafka-clients-sb-sample -o jsonpath='{.spec.host}')/consumer/kafka/messages?commit=true&partition=0" | jq
{
  "messages": [
    {
      "key": null,
      "timestamp": 1581087539350,
      "content": "Simple message",
      "partition": 0,
      "offset": 0
    },
    ...
    {
      "key": null,
      "timestamp": 1581087584266,
      "content": "Simple message",
      "partition": 0,
      "offset": 3
    }
  ]
}
```

With Minikube:

```shell script
❯ curl $(minikube ip):$(kubectl get svc kafka-clients-sb-sample -n amq-streams -o jsonpath='{.spec.ports[].nodePort}')"/consumer/kafka/messages?commit=true&partition=0"
{"messages":[{"key":null,"timestamp":1596203271368,"content":"Simple message from Minikube","partition":0,"offset":4}]}
```

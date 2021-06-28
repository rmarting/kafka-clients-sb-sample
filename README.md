# Kafka Clients on Spring Boot Sample

This sample project demonstrates how to use [Kafka Clients](https://mvnrepository.com/artifact/org.apache.kafka/kafka-clients)
and [Spring Kafka](https://spring.io/projects/spring-kafka) on Spring Boot to send and consume messages from an
[Apache Kafka](https://kafka.apache.org/) cluster. The Apache Kafka cluster is operated by [Strimzi](https://strimzi.io/)
operator deployed on a Kubernetes or OpenShift Platform. These messages will be validated by a Schema Registry or Service Registry
operated by [Apicurio](https://www.apicur.io/registry/docs/apicurio-registry/index.html#intro-to-the-registry) operator.

Apache Kafka is an open-sourced distributed event streaming platform for high-performance data pipelines,
streaming analytics, data integration, and mission-critical applications.

Service Registry is a datastore for sharing standard event schemas and API designs across API and event-driven architectures.
You can use Service Registry to decouple the structure of your data from your client applications, and to share and
manage your data types and API descriptions at runtime using a REST interface.

The example includes a simple REST API with the following operations:

* Send messages to a Topic
* Consume messages from a Topic

To deploy this application into a Kubernetes/OpenShift environment, we use the amazing [JKube](https://www.eclipse.org/jkube/).

## Environment

This project requires a Kubernetes or OpenShift platform available. If you do not have one, you could use 
one of the following resources to deploy locally a Kubernetes or OpenShift Cluster:

* [Red Hat CodeReady Containers - OpenShift 4 on your Laptop](https://github.com/code-ready/crc)
* [Minikube - Running Kubernetes Locally](https://kubernetes.io/docs/setup/minikube/)

This repo was tested with the following latest versions of Red Hat CodeReady Containers and Minikube:

```shell
❯ crc version
CodeReady Containers version: 1.28.0+08de64bd
OpenShift version: 4.7.13 (embedded in executable)
❯ minikube version
minikube version: v1.21.0
commit: 76d74191d82c47883dc7e1319ef7cebd3e00ee11
```

> Note: Whatever the platform you are using (Kubernetes or OpenShift), you could use the
> Kubernetes CLI (```kubectl```) or OpenShift CLI (```oc```) to execute the commands described in this repo.
> To reduce the length of this document, the commands displayed will use the Kubernetes CLI. When a specific
> command is only valid for Kubernetes or OpenShift it will be identified.

To deploy the resources, we will create a new ```amq-streams-demo``` namespace in the cluster in the case of Kubernetes:

```shell
❯ kubectl create namespace amq-streams-demo
```

If you are using OpenShift, then we will create a project:

```shell
❯ oc new-project amq-streams-demo
```

> Note: All the commands should be executed in this namespace. You could permanently save the namespace for
> all subsequent ```kubectl``` commands in that context.
>
> In Kubernetes:
>
> ```shell
> ❯ kubectl config set-context --current --namespace=amq-streams-demo
> ```
>
> In OpenShift:
>
> ```shell
> ❯ oc project amq-streams-demo
> ```

### Start Red Hat CodeReady Containers

To start up your local OpenShift 4 cluster:

```shell
❯ crc setup
❯ crc start -p /PATH/TO/your-pull-secret-file.json
```

You could promote `developer` user as `cluster-admin` with the following command:

```shell
❯ oc adm policy add-cluster-role-to-user cluster-admin developer
clusterrole.rbac.authorization.k8s.io/cluster-admin added: "developer"
```

### Start Minikube

To start up your local Kubernetes cluster:

```shell
❯ minikube start
❯ minikube addons enable ingress
❯ minikube addons enable registry
```

To install the OLM v0.18.2 in Kubernetes, execute the following commands:

```shell
❯ kubectl apply -f "https://github.com/operator-framework/operator-lifecycle-manager/releases/download/v0.18.2/crds.yaml"
❯ kubectl apply -f "https://github.com/operator-framework/operator-lifecycle-manager/releases/download/v0.18.2/olm.yaml"
```

> Note: There is an addon in minikube to install OLM, however at the moment of writing this
> repo, the latest version available not include the latest version of the operators.

### Deploying Strimzi and Apicurio Operators

> **NOTE**: Only *cluster-admin* users could deploy Kubernetes Operators. This section must
> be executed with one of them.

To deploy the Strimzi and Apicurio Operators only to inspect our namespace, we need to use
an ```OperatorGroup```. An OperatorGroup is an OLM resource that provides multitenant configuration to
OLM-installed Operators. For more information about this object, please review the
official documentation [here](https://docs.openshift.com/container-platform/4.7/operators/understanding/olm/olm-understanding-operatorgroups.html).

```shell
❯ kubectl apply -f src/main/olm/operator-group.yml
operatorgroup.operators.coreos.com/amq-streams-demo-og created
```

Now we are ready to deploy the Strimzi and Apicurio Operators:

For Kubernetes use the following subscriptions:

```shell
❯ kubectl apply -f src/main/strimzi/operator/subscription-k8s.yml
subscription.operators.coreos.com/strimzi-kafka-operator created
❯ kubectl apply -f src/main/apicurio/operator/subscription-k8s.yml
subscription.operators.coreos.com/apicurio-registry created
```

For OpenShift use the following subscriptions:

```shell
❯ oc apply -f src/main/strimzi/operator/subscription.yml
subscription.operators.coreos.com/strimzi-kafka-operator created
❯ oc apply -f src/main/apicurio/operator/subscription.yml 
subscription.operators.coreos.com/apicurio-registry created
```

You could check that operators are successfully registered with the following command:

```shell
❯ kubectl get csv
NAME                                             DISPLAY                      VERSION              REPLACES                           PHASE
apicurio-registry-operator.v1.0.0-v2.0.0.final   Apicurio Registry Operator   1.0.0-v2.0.0.final                                      Succeeded
strimzi-cluster-operator.v0.24.0                 Strimzi                      0.24.0               strimzi-cluster-operator.v0.23.0   Succeeded
```

or verify the pods are running:

```shell
❯ kubectl get pod
NAME                                              READY   STATUS    RESTARTS   AGE
apicurio-registry-operator-598fff6985-xplll       1/1     Running   0          47m
strimzi-cluster-operator-v0.24.0-9d5c6b6d-qqxjx   1/1     Running   0          47m
```

For more information about how to install Operators using the CLI command, please review this [article](
https://docs.openshift.com/container-platform/4.7/operators/admin/olm-adding-operators-to-cluster.html#olm-installing-operator-from-operatorhub-using-cli_olm-adding-operators-to-a-cluster)

### Deploying Kafka

```src/main/strimzi``` folder includes a set of custom resource to deploy a Kafka Cluster
and some Kafka Topics using the Strimzi Operators.

To deploy the Kafka Cluster:

```shell
❯ kubectl apply -f src/main/strimzi/kafka/kafka.yml
kafka.kafka.strimzi.io/my-kafka created
```

> If you want to deploy a Kafka Cluster with HA capabilities, there is a definition
> in [kafka-ha.yml](./src/main/strimzi/kafka/kafka-ha.yml) file.

To deploy the Kafka Topics:

```shell
❯ kubectl apply -f src/main/strimzi/topics/kafkatopic-messages.yml
kafkatopic.kafka.strimzi.io/messages created
```

> If you want to use a Kafka Topic with HA capabilities, there is a definition
> in [kafkatopic-messages-ha.yml](./src/main/strimzi/topics/kafkatopic-messages-ha.yml) file.

There is a set of different users to connect to Kafka Cluster. We will deploy here to be used later.

```shell
❯ kubectl apply -f src/main/strimzi/users/
kafkauser.kafka.strimzi.io/application created
kafkauser.kafka.strimzi.io/service-registry-scram created
kafkauser.kafka.strimzi.io/service-registry-tls created
```

After some minutes Kafka Cluster will be deployed:

```shell
❯ kubectl get pod
NAME                                              READY   STATUS    RESTARTS   AGE
apicurio-registry-operator-598fff6985-xplll       1/1     Running   0          51m
my-kafka-entity-operator-67b75cbd47-ccblt         3/3     Running   0          63s
my-kafka-kafka-0                                  1/1     Running   0          2m8s
my-kafka-zookeeper-0                              1/1     Running   0          3m27s
strimzi-cluster-operator-v0.23.0-9d5c6b6d-qqxjx   1/1     Running   0          51m
```

### Service Registry

Service Registry needs a set of Kafka Topics to store schemas and metadata of them. We need to execute the following
commands to create the KafkaTopics and to deploy an instance of Service Registry:

```shell
❯ kubectl apply -f src/main/apicurio/topics/
kafkatopic.kafka.strimzi.io/kafkasql-journal created
❯ kubectl apply -f src/main/apicurio/service-registry.yml
apicurioregistry.apicur.io/service-registry created
```

A new Deployment/DeploymentConfig is created with the prefix ```service-registry-deployment-``` and a new route is
created with the prefix ```service-registry-ingress-```. We must inspect it
to get the route created to expose the Service Registry API.

In Kubernetes we will use an ingress entry based with ```NodePort```. To get the ingress entry:

```shell
❯ kubectl get deployment | grep service-registry-deployment
service-registry-deployment
❯ kubectl expose deployment service-registry-deployment --type=NodePort --port=8080
service/service-registry-deployment exposed
❯ kubectl get service/service-registry-deployment
NAME                          TYPE       CLUSTER-IP      EXTERNAL-IP   PORT(S)          AGE
service-registry-deployment   NodePort   10.110.126.48   <none>        8080:30456/TCP   15s 
❯ minikube service service-registry-deployment --url -n amq-streams-demo
http://192.168.50.115:30456
```

In OpenShift, we only need to check the ```host``` attribute from the OpenShift Route:

```shell
❯ oc get route
NAME                             HOST/PORT                                            PATH   SERVICES                   PORT    TERMINATION   WILDCARD
service-registry-ingress-b828r   service-registry.amq-streams-demo.apps-crc.testing   /      service-registry-service   <all>                 None
```

While few minutes until your Service Registry has deployed.

The Service Registry Web Console and API endpoints will be available from: 

* **Web Console**: http://<KUBERNETES_OPENSHIFT_SR_ROUTE_SERVICE_HOST>/ui/
* **API REST**: http://KUBERNETES_OPENSHIFT_SR_ROUTE_SERVICE_HOST/apis/registry/v2

Set up the ```apicurio.registry.url``` property in the ```pom.xml``` file the Service Registry url before to publish the
schemas used by this application:

```shell
❯ oc get route -l app=service-registry -o jsonpath='{.items[0].spec.host}'
```

To register the schemas in Service Registry running in Kubernetes:

```shell
❯ mvn clean generate-sources -Papicurio \
  -Dapicurio.registry.url=$(minikube service service-registry-deployment --url -n amq-streams-demo)/apis/registry/v2
```

To register the schemas in Service Registry running in OpenShift:

```shell
❯ mvn clean generate-sources -Papicurio
```

The next screenshot shows the schemas registered in the Web Console:

![Artifacts registered in Apicurio Registry](./img/apicurio-registry-artifacts.png) 

# Build and Deploy

Before we build the application we need to set up some values in ```src/main/resources/application.properties``` file.

Review and set up the right values from your Kafka Cluster 

* **Kafka Bootstrap Servers**: Kafka brokers are defined by a Kubernetes or OpenShift service created by Strimzi when
the Kafka cluster is deployed. This service, called *cluster-name*-kafka-bootstrap exposes 9092 port for plain
traffic and 9093 for encrypted traffic. 

```text
kafka.bootstrap-servers = my-kafka-kafka-bootstrap:9092

spring.kafka.bootstrap-servers = ${kafka.bootstrap-servers}
```

* **Kafka User Credentials**: Kafka Cluster requires authentication and we need to set up the Kafka User credentials
in our application (```kafka.user.*``` properties in ```application.properties``` file). Each KafkaUser has its own
secret to store the password. This secret must be checked to extract the password for our user.

To extract the password of the KafkaUser and declare as Environment Variable:

```shell
❯ export KAFKA_USER_PASSWORD=$(kubectl get secret application -o jsonpath='{.data.password}' | base64 -d)
```

It is a best practice use directly the secret as variable in our deployment in Kubernetes or OpenShift. We could do
it declaring the variable in the container spec as:

```yaml
spec:
  template:
    spec:
      containers:
        - env:
          - name: KAFKA_USER_PASSWORD
            valueFrom:
              secretKeyRef:
                key: password
                name: application
```

There is a deployment definition in [deployment.yml](./src/main/jkube/deployment.yml) file. This file will be used
by JKube to deploy our application in Kubernetes or OpenShift.

* **Service Registry API Endpoint**: Avro Serde classes need to communicate with the Service Registry API to check and
validate the schemas. 

```text
apicurio.registry.url = http://service-registry.amq-streams-demo.apps-crc.testing/apis/registry/v2
```

To build the application:

```shell
❯ mvn clean package
```

To run locally:

```shell
❯ export KAFKA_USER_PASSWORD=$(kubectl get secret application -o jsonpath='{.data.password}' | base64 -d)
❯ mvn spring-boot:run
```

Or you can deploy into Kubernetes or OpenShift platform using [Eclipse JKube](https://github.com/eclipse/jkube) Maven Plug-ins:

To deploy the application using the Kubernetes Maven Plug-In:

```shell
❯ mvn package k8s:resource k8s:build k8s:push k8s:apply -Pkubernetes -Djkube.build.strategy=jib
```

To deploy the application using the OpenShift Maven Plug-In (only valid for OpenShift Platform):

```shell
❯ oc adm policy add-role-to-user view system:serviceaccount:amq-streams-demo:default
❯ mvn package oc:resource oc:build oc:apply -Popenshift
```

To deploy the application in Minikube:

```shell
❯ eval $(minikube docker-env)
❯ kubectl create -f src/main/k8s/role.yml
❯ mvn package k8s:resource k8s:build k8s:apply -Pkubernetes
```

# REST API

REST API is available from a Swagger UI at:

```text
http://<KUBERNETES_OPENSHIFT_ROUTE_SERVICE_HOST>/swagger-ui.html
```

**KUBERNETES_OPENSHIFT_ROUTE_SERVICE_HOST** will be the route create on Kubernetes or OpenShift to expose outside the
service.

To get the route the following command in OpenShift give you the host:

```shell
❯ oc get route kafka-clients-sb-sample -o jsonpath='{.spec.host}'
```

There are two groups to manage a topic from a Kafka Cluster.

* **Producer**: Send messageDTOS to a topic 
* **Consumer**: Consume messageDTOS from a topic

## Producer REST API

Sample REST API to send messages to a Kafka Topic.
Parameters:

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

```shell
❯ curl -X POST http://$(oc get route kafka-clients-sb-sample -o jsonpath='{.spec.host}')/producer/kafka/messages \
-H "Content-Type:application/json" -d '{"content": "Simple message"}' | jq
{
  "key": null,
  "timestamp": 1581087543362,
  "content": "Simple message",
  "partition": 0,
  "offset": 3
}
```

With Minikube:

```shell
❯ curl $(minikube ip):$(kubectl get svc kafka-clients-sb-sample -o jsonpath='{.spec.ports[].nodePort}')/producer/kafka/messages \
-H "Content-Type:application/json" -d '{"content": "Simple message from Minikube"}' | jq
{
  "key": null,
  "timestamp": 1596203271368,
  "content": "Simple message from Minikube",
  "partition": 0,
  "offset": 4
}
```

## Consumer REST API

Sample REST API to consume messages from a Kafka Topic.
Parameters:

* **topicName**: Topic Name (Required)
* **partition**: Number of the partition to consume (Optional)
* **commit**: Commit messaged consumed. Values: true|false

Simple Sample:

```shell
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

```shell
❯ curl $(minikube ip):$(kubectl get svc kafka-clients-sb-sample -o jsonpath='{.spec.ports[].nodePort}')"/consumer/kafka/messages?commit=true&partition=0" | jq
{
  "messages":[
    {
      "key": null,
      "timestamp": 1596203271368,
      "content": "Simple message from Minikube",
      "partition": 0,
      "offset": 4
    }
  ]
}
```

That is! You have been deployed a full stack of components to produce and consume checked and valid messages using
a schema declared. Congratulations!.

## Main References

* [Strimzi](https://strimzi.io/)
* [Apicurio](https://www.apicur.io/)
* [OperatorHub - Strimzi](https://operatorhub.io/operator/strimzi-kafka-operator)
* [OperatorHub - Apicurio Registry](https://operatorhub.io/operator/apicurio-registry)

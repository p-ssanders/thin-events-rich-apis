#   Thin Events / Rich APIs Reference Implementation

This repository contains a reference implementation that demonstrates the "thin events / rich apis" integration pattern
using Spring Boot and RabbitMQ.

There are two applications:
*   `publisher`
*   `consumer`

The `publisher` publishes events when its state changes (e.g.: the collection of entities it manages changes), and the
`consumer` consumes those events. Assume that the consumer's bounded context has some mapping to the publisher's
bounded context, and the events are how the publisher notifies the consumer of changes in state so that the consumer
can update itself accordingly. For simplicity there is no actual domain implemented to demonstrate the assumed context
mapping.

The published events are "thin" meaning they don't contain enough information for a consumer to update its own state
accordingly. Instead, the events contain just enough information to inform a consumer of how to get the information it
might need to update its own state. In this example, the event payload contains the URL of the entity that changed, and
a timestamp of when the event occurred. The consumer can make a request to the URL to get the information it needs to
update itself.

The RabbitMQ [Fanout Exchange](https://www.rabbitmq.com/tutorials/amqp-concepts.html#exchange-fanout) is used so that
the publisher can be certain events will be published to whoever is listening at the moment, even if it's no one. The
publisher doesn't care if anyone is listening because its own internal state is consistent; it's publishing events to be
a good participant in a system, allowing other applications to react to change in its state, very similar to how an
aggregate root publishes domain events.

This implementation also includes patterns to guarantee eventual event publication such as the persistence of events
themselves with publication metadata, the delegation of responsibility of event publication to a background thread, and
RabbitMQ publisher confirms. The consumer's implementation guarantees eventual reaction to events in a similar way such
as the persistence of received events, the delegation of responsibility of reaction to received events to a background
thread, and consumer acknowledgments.

![system-visualization](system-visualization.jpg)

##  Build

`./gradlew clean build`

##  Run

Run a RabbitMQ instance
```shell
docker run -d --hostname rabbitmq --name demo -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```

Run the publisher
```shell
./gradlew :publisher:bootRun
```

Run the consumer
```shell
./gradlew :consumer:bootRun
```

##  Interact

Make a `Thing` to publish an event:

```http request
POST http://localhost:8080/things
Content-Type: application/json

{ "content": "some-content-000" }
```

##  Observe

Distributed applications are hard to observe. Even in this trivialized example there are two applications, each with
their own HTTP APIs, messages flowing between them brokered by a third application (RabbitMQ), and requests being made
from one to another.

The `publisher` and `consumer` applications provide three ways to increase observability, and ameliorate maintenance and
operations of the system:

1.  Meaningful log messages, logged at the appropriate level. Tail the logs of each application to gain insight into
    what is happening as it happens.
1.  Spring Boot Actuator endpoints related to RabbitMQ:

    *   http://localhost:8080/actuator/metrics
    *   http://localhost:8081/actuator/metrics

    Each of these metrics can be queried to gain additional insight into application state. For example:

    *   Make a request to http://localhost:8080/actuator/metrics/rabbitmq.published to see the count of published events
    *   Make a request to http://localhost:8081/actuator/metrics/rabbitmq.acknowledged to see the count of acknowledged
        events
1.  A database web console (for development purposes only). Query the database tables to observe how the state changes
    as events are published and consumed.

    *   http://localhost:8080/h2-console
        *   JDBC URL: jdbc:h2:mem:publisher
        *   Username: sa
        *   Password:
    *   http://localhost:8081/h2-console
        *   JDBC URL: jdbc:h2:mem:consumer
        *   Username: sa
        *   Password:

##  Test

The functionality of the `publisher` and `consumer` applications is verified using automated tests that apply two
distinct methodologies:

1.  Black-box application tests
1.  Contract tests

> Using this combination of using narrow integration tests and contract tests, I can be confident of integrating against an external service without ever running tests against a real instance of that service, which greatly eases my build process.
 -Martin Fowler

### Black-box Application Tests

The `publisher` application has a suite of integration tests in [PublisherApplicationTests](publisher/src/test/java/dev/samsanders/demo/rabbitmq/publisher/PublisherApplicationTests.java)

The primary focus of these tests is how information is published.

Note these tests use an embedded [Apache Qpid](https://qpid.apache.org/) AMQP broker, and that this embedded AMQP broker
is not RabbitMQ. This decision accepts the tradeoff of speed and portability in favor of dev/prod-parity, relying on the
AMQP standard as a mitigation. The embedded nature of the broker also obviates the need for a dedicated AMQP broker,
network connectivity for automated tests, or additional environmental requirements (e.g.: Docker).
This decision not based on a strongly-held opinion, so let me know if you have alternative ideas!

The `consumer` doesn't have this type of test because all it does is consume information from the `publisher`, and that
can be tested using contract tests. A real consumer application with actual functionality would probably need one to
test that functionality.

### Contract Tests

#### Publisher

The `publisher` application's ability to create and publish events is verified by the black-box application tests, but
what about _what_ is published?

The `publisher` application contains a contract definition that specifies what is published, and to where. The contract
is used by Spring Cloud Contract at build time to generate contract tests that validate the application fulfills the
terms of the contract. Additionally, the contract itself is built into an additional "stub" jar that can be published
somewhere that consumers can access, so that consumers can use the contract to validate their integrations rather than
needing access to an instance of `publisher`.

For details review the `publisher` [messaging contract](publisher/src/contractTest/resources/contracts/messaging/publishThingEvent.yml)
, the [generated contract tests](publisher/build/generated-test-sources/contractTest/java/dev/samsanders/demo/rabbitmq/publisher/contracts/MessagingTest.java)
, and observe the generated [stubs jar](publisher/build/libs/) alongside the application jar.
Note the `publisher` also has contracts, and tests for its web API so that consumers of the messages/events can also
build a reaction to those events with confidence.

#### Consumer

The `consumer` application intends to consume events published by `publisher`, and `consumer` intends to react to the
events by making requests to the `publisher`'s web API. Rather than building these integrations against an instance of
the `publisher`, the `consumer` can declare a dependency on the `publisher`'s stub jar, download some version of the
contracts, and use Spring Cloud Contract in automated tests to mock the publication of messages and web APIs based on
the contract definition.

For details review the [`ConsumerStubTests`](consumer/src/contractTest/java/dev/samsanders/demo/rabbitmq/consumer/ConsumerStubTests.java).

##  References

*   [AMQP 0-9-1 Model Explained](https://www.rabbitmq.com/tutorials/amqp-concepts.html)
*   [Spring AMQP Reference Documentation](https://docs.spring.io/spring-amqp/reference/html)
*   [Spring Cloud Contract Reference Documentation](https://cloud.spring.io/spring-cloud-contract/reference/html/index.html)
*   [Spring Cloud Contract Stub Runner](https://cloud.spring.io/spring-cloud-contract/reference/html/project-features.html#features-stub-runner)
*   [Maven Publish Plugin](https://docs.gradle.org/current/userguide/publishing_maven.html#publishing_maven)
*   [Integration Tests](https://martinfowler.com/bliki/IntegrationTest.html)

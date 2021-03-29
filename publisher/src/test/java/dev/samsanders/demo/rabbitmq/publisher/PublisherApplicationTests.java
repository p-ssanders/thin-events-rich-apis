package dev.samsanders.demo.rabbitmq.publisher;

import dev.samsanders.demo.rabbitmq.publisher.app.ThingEventConfirmCallback;
import dev.samsanders.demo.rabbitmq.publisher.app.ThingEventPublisher;
import dev.samsanders.demo.rabbitmq.publisher.thing.ThingEvent;
import dev.samsanders.demo.rabbitmq.publisher.thing.ThingEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.util.StreamUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@ContextConfiguration(initializers = PublisherApplicationTests.RabbitMQInitializer.class)
class PublisherApplicationTests {

    private static final String BASE_URL = "http://localhost:%d/things";
    private static final int RABBITMQ_PORT = 5672;
    @Container
    private static final GenericContainer<?> RABBITMQ =
            new GenericContainer<>(DockerImageName.parse("rabbitmq:3-management"))
                    .withExposedPorts(RABBITMQ_PORT);

    @Autowired
    TestRestTemplate testRestTemplate;

    @Autowired
    ThingEventRepository thingEventRepository;

    @Autowired
    ThingEventPublisher thingEventPublisher;

    @Autowired
    ThingEventConfirmCallback thingEventConfirmCallback;

    @LocalServerPort
    int port;

    @Test
    void happyPath() throws InterruptedException {
        // Create a Thing
        RequestEntity<String> createThingRequest = RequestEntity
                .post(URI.create(String.format(BASE_URL, port)))
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"content\": \"some-content\"}");
        ResponseEntity<Void> createThingResponse = testRestTemplate.exchange(createThingRequest, Void.class);

        // Expect an unpublished ThingEvent (scheduling is disabled for tests)
        assertEquals(HttpStatus.CREATED, createThingResponse.getStatusCode());
        long thingId = getThingIdFromResponse(createThingResponse);
        List<ThingEvent> thingEvents =
                StreamUtils.createStreamFromIterator(thingEventRepository.findAllByThingId(thingId).iterator())
                        .collect(Collectors.toList());
        assertEquals(1, thingEvents.size());
        assertNull(thingEvents.get(0).getPublishedInstant());

        // Publish the ThingEvent manually (scheduling is disabled for tests)
        // Use a CountDownLatch to know when the ConfirmCallback was invoked
        CountDownLatch countDownLatch = new CountDownLatch(1);
        thingEventConfirmCallback.setCountDownLatch(countDownLatch);
        thingEventPublisher.publishAllUnpublishedThingEvents();

        // Expect ThingEvent was marked as published
        countDownLatch.await(1000L, TimeUnit.MILLISECONDS);
        assertEquals(0, countDownLatch.getCount());
        thingEvents =
                StreamUtils.createStreamFromIterator(thingEventRepository.findAllByThingId(1L).iterator())
                        .collect(Collectors.toList());
        assertEquals(1, thingEvents.size());
        assertNotNull(thingEvents.get(0).getPublishedInstant());

    }

    @Test
    void sadPath_brokerUnavailable() {
        // Create a Thing
        RequestEntity<String> createThingRequest = RequestEntity
                .post(URI.create(String.format(BASE_URL, port)))
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"content\": \"some-content\"}");
        ResponseEntity<Void> createThingResponse = testRestTemplate.exchange(createThingRequest, Void.class);

        // Stop RabbitMQ
        RABBITMQ.stop();
        // Publish the ThingEvent manually (scheduling is disabled for tests)
        // Expect an exception to be thrown
        long thingId = getThingIdFromResponse(createThingResponse);
        try {
            thingEventPublisher.publishAllUnpublishedThingEvents();
        } catch (AmqpException e) {
            // Expect ThingEvent was not marked as published
            List<ThingEvent> thingEvents =
                    StreamUtils.createStreamFromIterator(thingEventRepository.findAllByThingId(thingId).iterator())
                            .collect(Collectors.toList());
            assertEquals(1, thingEvents.size());
            assertNull(thingEvents.get(0).getPublishedInstant());
        }
    }

    private long getThingIdFromResponse(ResponseEntity<Void> createThingResponse) {
        String thingLocationPath = createThingResponse.getHeaders().getLocation().getPath();
        return Long.parseLong(thingLocationPath.substring(thingLocationPath.lastIndexOf("/") + 1));
    }

    static class RabbitMQInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertyValues testPropertyValues = TestPropertyValues.of(
                    "spring.rabbitmq.host=" + RABBITMQ.getContainerIpAddress(),
                    "spring.rabbitmq.port=" + RABBITMQ.getMappedPort(RABBITMQ_PORT)
            );
            testPropertyValues.applyTo(applicationContext);
        }
    }
}
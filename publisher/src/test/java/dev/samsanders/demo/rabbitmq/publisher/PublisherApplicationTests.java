package dev.samsanders.demo.rabbitmq.publisher;

import dev.samsanders.demo.rabbitmq.publisher.app.ThingEventConfirmCallback;
import dev.samsanders.demo.rabbitmq.publisher.app.ThingEventPublisher;
import dev.samsanders.demo.rabbitmq.publisher.thing.ThingEvent;
import dev.samsanders.demo.rabbitmq.publisher.thing.ThingEventRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.amqp.AmqpException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.data.util.StreamUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(Lifecycle.PER_CLASS)
class PublisherApplicationTests {

    private static final String BASE_URL = "http://localhost:%d/things";
    private String baseUrlWithPort;

    @Autowired
    EmbeddedAmqpBroker embeddedAmqpBroker;

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

    @BeforeAll
    void beforeAll() {
        baseUrlWithPort = String.format(BASE_URL, port);
    }

    @Test
    void happyPath() throws InterruptedException {
        // Create a Thing
        RequestEntity<String> createThingRequest = RequestEntity
                .post(URI.create(baseUrlWithPort))
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

        // Start the AMQP broker
        // Publish the ThingEvent manually (scheduling is disabled for tests)
        // Use a CountDownLatch to know when the ConfirmCallback was invoked
        embeddedAmqpBroker.start();
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

        embeddedAmqpBroker.stop();
    }

    @Test
    void sadPath_brokerUnavailable() {
        // Create a Thing
        RequestEntity<String> createThingRequest = RequestEntity
                .post(URI.create(baseUrlWithPort))
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"content\": \"some-content\"}");
        ResponseEntity<Void> createThingResponse = testRestTemplate.exchange(createThingRequest, Void.class);

        // Don't start the AMQP broker
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
        long thingId = Long.parseLong(thingLocationPath.substring(thingLocationPath.lastIndexOf("/") + 1));
        return thingId;
    }

}

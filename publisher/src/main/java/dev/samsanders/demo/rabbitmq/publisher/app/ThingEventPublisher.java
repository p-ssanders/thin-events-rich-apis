package dev.samsanders.demo.rabbitmq.publisher.app;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.samsanders.demo.rabbitmq.publisher.thing.ThingEvent;
import dev.samsanders.demo.rabbitmq.publisher.thing.ThingEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import java.net.URI;
import java.time.Instant;

public class ThingEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(ThingEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final ThingEventRepository thingEventRepository;
    private final String baseUrl;

    public ThingEventPublisher(RabbitTemplate rabbitTemplate, ThingEventRepository thingEventRepository,
                               String baseUrl) {
        this.rabbitTemplate = rabbitTemplate;
        this.thingEventRepository = thingEventRepository;
        this.baseUrl = baseUrl;
    }

    @Scheduled(fixedRate = 2000L)
    public void publishAllUnpublishedThingEvents() {
        logger.info("Getting all unpublished ThingEvents");

        Iterable<ThingEvent> unpublishedEvents = thingEventRepository
                .findAllByPublishedInstantIsNullOrderByCreatedInstant();

        unpublishedEvents.forEach(thingEvent -> {
            ThingEventDto thingEventDto = new ThingEventDto(thingEvent, baseUrl);
            logger.info(String.format("Publishing ThingEvent: %s", thingEventDto));
            rabbitTemplate
                    .convertAndSend(thingEventDto, (message) -> message,
                            new CorrelationData(String.valueOf(thingEvent.getId()))
                    );
        });
    }

    private static class ThingEventDto {

        private final URI thingUri;
        private final Instant createdInstant;

        private ThingEventDto(ThingEvent thingEvent, String baseUrl) {
            this.thingUri = URI.create(String.format("%s/things/%d", baseUrl, thingEvent.getThingId()));
            this.createdInstant = thingEvent.getCreatedInstant();
        }

        @JsonCreator
        ThingEventDto(@JsonProperty("thingUri") URI thingUri,
                      @JsonProperty("createdInstant") Instant createdInstant) {
            this.thingUri = thingUri;
            this.createdInstant = createdInstant;
        }

        public URI getThingUri() {
            return thingUri;
        }

        public Instant getCreatedInstant() {
            return createdInstant;
        }

        @Override
        public String toString() {
            return "ThingEventDto{" +
                    "thingUri=" + thingUri +
                    ", createdInstant=" + createdInstant +
                    '}';
        }
    }

}

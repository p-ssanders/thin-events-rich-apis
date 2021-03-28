package dev.samsanders.demo.rabbitmq.consumer.app;

import dev.samsanders.demo.rabbitmq.consumer.thing.Thing;
import dev.samsanders.demo.rabbitmq.consumer.thing.ThingEvent;
import dev.samsanders.demo.rabbitmq.consumer.thing.ThingEventRepository;
import dev.samsanders.demo.rabbitmq.consumer.thing.ThingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

public class ThingGetter {

    private static final Logger logger = LoggerFactory.getLogger(ThingGetter.class);

    private final ThingEventRepository thingEventRepository;
    private final RestTemplate restTemplate;
    private final ThingRepository thingRepository;


    public ThingGetter(ThingEventRepository thingEventRepository, RestTemplate restTemplate,
                       ThingRepository thingRepository) {
        this.thingEventRepository = thingEventRepository;
        this.restTemplate = restTemplate;
        this.thingRepository = thingRepository;
    }

    @Scheduled(fixedRate = 2000L)
    @Transactional
    public void getAllUnconsumedThings() {
        logger.info("Getting all unconsumed ThingEvents");

        Iterable<ThingEvent> thingEvents = thingEventRepository
                .findAllByConsumedInstantIsNullOrderByCreatedInstant();

        thingEvents.forEach(thingEvent -> {
            RequestEntity<Void> thingRequest =
                    RequestEntity
                            .get(thingEvent.getThingUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .build();

            logger.info(String.format("Getting Thing %s", thingEvent.getThingUri()));

            ResponseEntity<Thing> thingResponseEntity = restTemplate.exchange(thingRequest, Thing.class);
            Thing thing = thingResponseEntity.getBody();

            logger.info(String.format("Got Thing: %s", thing));

            thingRepository.save(thing);
            thingEvent.setConsumedInstant(Instant.now());
            thingEventRepository.save(thingEvent);
        });
    }
}

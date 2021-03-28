package dev.samsanders.demo.rabbitmq.publisher.app;

import dev.samsanders.demo.rabbitmq.publisher.thing.ThingEvent;
import dev.samsanders.demo.rabbitmq.publisher.thing.ThingEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate.ConfirmCallback;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

public class ThingEventConfirmCallback implements ConfirmCallback {

    private static final Logger logger = LoggerFactory.getLogger(ThingEventConfirmCallback.class);
    private final ThingEventRepository thingEventRepository;
    private CountDownLatch countDownLatch;

    public ThingEventConfirmCallback(ThingEventRepository thingEventRepository) {
        this.thingEventRepository = thingEventRepository;
    }

    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        if (correlationData == null) {
            throw new IllegalStateException("Can't confirm event with null CorrelationData");
        }

        logger.info(String.format("ThingEvent publish confirmed: %s %s", correlationData, ack));

        Long eventId = Long.valueOf(correlationData.getId());
        Optional<ThingEvent> optionalThingEvent = thingEventRepository.findById(eventId);

        if (optionalThingEvent.isEmpty()) {
            throw new IllegalStateException(String.format("Can't find ThingEvent with id: %d", eventId));
        }

        ThingEvent thingEvent = optionalThingEvent.get();
        thingEvent.setPublishedInstant(Instant.now());
        thingEventRepository.save(thingEvent);

        if (countDownLatch != null) {
            countDownLatch.countDown();
        }
    }

    public void setCountDownLatch(CountDownLatch countDownLatch) {
        this.countDownLatch = countDownLatch;
    }
}

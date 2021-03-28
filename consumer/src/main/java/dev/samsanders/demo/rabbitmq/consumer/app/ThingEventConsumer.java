package dev.samsanders.demo.rabbitmq.consumer.app;

import com.rabbitmq.client.Channel;
import dev.samsanders.demo.rabbitmq.consumer.thing.ThingEvent;
import dev.samsanders.demo.rabbitmq.consumer.thing.ThingEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;

public class ThingEventConsumer implements ChannelAwareMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(ThingEventConsumer.class);
    private final Jackson2JsonMessageConverter messageConverter;
    private final ThingEventRepository thingEventRepository;
    private CountDownLatch countDownLatch;

    public ThingEventConsumer(Jackson2JsonMessageConverter messageConverter, ThingEventRepository thingEventRepository) {
        this.messageConverter = messageConverter;
        this.thingEventRepository = thingEventRepository;
    }

    @Override
    @Transactional
    public void onMessage(Message message, Channel channel) throws IOException {
        logger.info(String.format("Received event: %s", message));
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            ThingEventDto thingEventDto = (ThingEventDto) messageConverter.fromMessage(message,
                    new ParameterizedTypeReference<ThingEventDto>() {
                    });
            ThingEvent thingEvent = new ThingEvent(thingEventDto.getThingUri(), thingEventDto.getCreatedInstant());
            thingEventRepository.save(thingEvent);
        } catch (Exception e) {
            logger.error("Exception caught: ", e);
            channel.basicNack(deliveryTag, false, false);
        }

        channel.basicAck(deliveryTag, false);

        if (countDownLatch != null) {
            countDownLatch.countDown();
        }
    }

    public void setCountDownLatch(CountDownLatch countDownLatch) {
        this.countDownLatch = countDownLatch;
    }

    private static final class ThingEventDto {

        private URI thingUri;
        private Instant createdInstant;

        public URI getThingUri() {
            return thingUri;
        }

        public void setThingUri(URI thingUri) {
            this.thingUri = thingUri;
        }

        public Instant getCreatedInstant() {
            return createdInstant;
        }

        public void setCreatedInstant(Instant createdInstant) {
            this.createdInstant = createdInstant;
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

package dev.samsanders.demo.rabbitmq.publisher.contracts;

import dev.samsanders.demo.rabbitmq.publisher.app.ThingEventPublisher;
import dev.samsanders.demo.rabbitmq.publisher.thing.ThingEvent;
import dev.samsanders.demo.rabbitmq.publisher.thing.ThingEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@AutoConfigureMessageVerifier
@ActiveProfiles("contract-test")
public class MessagingBase {

    @Autowired
    ThingEventRepository thingEventRepository;

    @Autowired
    ThingEventPublisher thingEventPublisher;

    void publishAllUnpublishedThingEvents() {
        thingEventRepository.save(new ThingEvent(1L));
        thingEventPublisher.publishAllUnpublishedThingEvents();
    }

}

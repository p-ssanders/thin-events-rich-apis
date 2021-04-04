package dev.samsanders.demo.rabbitmq.consumer;

import dev.samsanders.demo.rabbitmq.consumer.app.ThingEventConsumer;
import dev.samsanders.demo.rabbitmq.consumer.thing.ThingEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.StubTrigger;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureStubRunner(
        ids = {"dev.samsanders.demo.rabbitmq:publisher:0.0.1-SNAPSHOT:stubs"},
        stubsMode = StubRunnerProperties.StubsMode.LOCAL
)
@ActiveProfiles("test")
public class ConsumerStubTests {

    @Autowired
    StubTrigger stubTrigger;

    @Autowired
    ThingEventConsumer thingEventConsumer;

    @Autowired
    ThingEventRepository thingEventRepository;

    @Test
    void consumeThingEvent() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        thingEventConsumer.setCountDownLatch(countDownLatch);

        stubTrigger.trigger("publish-thing-event");

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);
        assertEquals(0, countDownLatch.getCount());
        assertEquals(2, thingEventRepository.count()); // FIXME not sure why it publishes twice
    }

    // TODO test getting the Thing via web API stub

}

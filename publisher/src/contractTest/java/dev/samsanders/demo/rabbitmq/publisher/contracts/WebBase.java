package dev.samsanders.demo.rabbitmq.publisher.contracts;

import dev.samsanders.demo.rabbitmq.publisher.thing.Thing;
import dev.samsanders.demo.rabbitmq.publisher.thing.ThingRepository;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMessageVerifier
@AutoConfigureMockMvc
public class WebBase {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    ThingRepository thingRepository;

    @BeforeEach
    void before() {
        RestAssuredMockMvc.mockMvc(mockMvc);

        Thing thing = new Thing();
        thing.setId(1);
        thing.setContent("some-content");
        when(thingRepository.save(any())).thenReturn(thing);
        when(thingRepository.findById(any())).thenReturn(Optional.of(thing));
    }

}

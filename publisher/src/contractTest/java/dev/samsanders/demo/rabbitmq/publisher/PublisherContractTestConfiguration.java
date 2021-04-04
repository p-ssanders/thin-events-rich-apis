package dev.samsanders.demo.rabbitmq.publisher;

import dev.samsanders.demo.rabbitmq.publisher.app.ThingEventPublisher;
import dev.samsanders.demo.rabbitmq.publisher.thing.ThingEventRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("contract-test")
public class PublisherContractTestConfiguration {

    @Bean
    ThingEventPublisher thingEventPublisher(
            RabbitTemplate rabbitTemplate,
            ThingEventRepository thingEventRepository,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter,
            @Value("${publisher.exchange-name}") String exchangeName,
            @Value("${publisher.base-url}") String baseUrl) {

        rabbitTemplate.setExchange(exchangeName);
        rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter);

        return new ThingEventPublisher(rabbitTemplate, thingEventRepository, baseUrl);
    }

}

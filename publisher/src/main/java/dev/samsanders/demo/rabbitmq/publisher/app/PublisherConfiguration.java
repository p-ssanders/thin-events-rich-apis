package dev.samsanders.demo.rabbitmq.publisher.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.samsanders.demo.rabbitmq.publisher.thing.ThingEventRepository;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory.ConfirmType;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
public class PublisherConfiguration {

    @Profile("!test")
    @EnableScheduling
    public static class SchedulingConfiguration {

    }

    @Bean
    FanoutExchange exchange(@Value("${publisher.exchange-name}") String exchangeName) {
        return new FanoutExchange(exchangeName);
    }

    @Profile("!contract-test")
    @Bean
    ThingEventPublisher thingEventPublisher(
            CachingConnectionFactory cachingConnectionFactory,
            ThingEventConfirmCallback thingEventConfirmCallback,
            RabbitTemplate rabbitTemplate,
            ThingEventRepository thingEventRepository,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter,
            @Value("${publisher.base-url}") String baseUrl,
            @Value("${publisher.exchange-name}") String exchangeName) {

        cachingConnectionFactory.setPublisherConfirmType(ConfirmType.CORRELATED);
        rabbitTemplate.setConnectionFactory(cachingConnectionFactory);
        rabbitTemplate.setConfirmCallback(thingEventConfirmCallback);
        rabbitTemplate.setExchange(exchangeName);

        return new ThingEventPublisher(rabbitTemplate, thingEventRepository, baseUrl);
    }

    @Bean
    ThingEventConfirmCallback thingEventConfirmCallback(ThingEventRepository thingEventRepository) {
        return new ThingEventConfirmCallback(thingEventRepository);
    }

    @Bean
    Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        return new Jackson2JsonMessageConverter(objectMapper);
    }

}

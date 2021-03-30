package dev.samsanders.demo.rabbitmq.consumer.app;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.samsanders.demo.rabbitmq.consumer.thing.ThingEventRepository;
import dev.samsanders.demo.rabbitmq.consumer.thing.ThingRepository;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.DirectMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ConsumerConfiguration {

    @Profile("!test")
    @EnableScheduling
    public static class SchedulingConfiguration {
    }

    @Bean
    FanoutExchange exchange(@Value("${consumer.exchange-name}") String exchangeName) {
        return new FanoutExchange(exchangeName);
    }

    @Bean
    Queue queue() {
        return new Queue("consumer-thing-events", true, false, false);
    }

    @Bean
    Binding binding(Queue queue, FanoutExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange);
    }

    @Bean
    MessageListenerContainer messageListenerContainer(AbstractMessageListenerContainer messageListenerContainer,
                                                      Queue queue,
                                                      ThingEventConsumer thingEventConsumer) {
        messageListenerContainer.setQueueNames(queue.getName());
        messageListenerContainer.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        messageListenerContainer.setMessageListener(thingEventConsumer);

        return messageListenerContainer;
    }

    @Profile("!test")
    @Bean
    AbstractMessageListenerContainer abstractMessageListenerContainer(ConnectionFactory connectionFactory) {
        return new DirectMessageListenerContainer(connectionFactory);
    }

    @Bean
    ThingEventConsumer thingEventConsumer(Jackson2JsonMessageConverter jackson2JsonMessageConverter,
                                          ThingEventRepository thingEventRepository) {
        return new ThingEventConsumer(jackson2JsonMessageConverter, thingEventRepository);
    }

    @Bean
    Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, true);

        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    ThingGetter thingGetter(ThingEventRepository thingEventRepository, RestTemplate restTemplate,
                            ThingRepository thingRepository) {
        return new ThingGetter(thingEventRepository, restTemplate, thingRepository);
    }

    @Bean
    RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder.build();
    }

}

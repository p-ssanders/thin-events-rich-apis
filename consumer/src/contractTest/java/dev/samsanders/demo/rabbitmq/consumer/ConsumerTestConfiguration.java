package dev.samsanders.demo.rabbitmq.consumer;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test")
public class ConsumerTestConfiguration {

    @Bean
    AbstractMessageListenerContainer abstractMessageListenerContainer(ConnectionFactory connectionFactory) {
        return new SimpleMessageListenerContainer(connectionFactory);
    }

}

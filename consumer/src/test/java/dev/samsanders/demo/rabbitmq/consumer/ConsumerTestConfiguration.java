package dev.samsanders.demo.rabbitmq.consumer;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.amqp.rabbit.connection.AbstractConnectionFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.NonNull;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Configuration
@Profile("contract-test")
public class ConsumerTestConfiguration {

    @Bean
    AbstractMessageListenerContainer abstractMessageListenerContainer(ConnectionFactory connectionFactory) {
        return new SimpleMessageListenerContainer(connectionFactory);
    }

    @Bean
    public ConnectionFactory connectionFactory() {
        final Connection mockConnection = mock(Connection.class);
        final AMQP.Queue.DeclareOk mockDeclareOk = mock(AMQP.Queue.DeclareOk.class);
        com.rabbitmq.client.ConnectionFactory mockConnectionFactory = mock(
                com.rabbitmq.client.ConnectionFactory.class, new Answer() {
                    @Override
                    public Object answer(InvocationOnMock invocationOnMock)
                            throws Throwable {
                        // hack for keeping backward compatibility with #303
                        if ("newConnection"
                                .equals(invocationOnMock.getMethod().getName())) {
                            return mockConnection;
                        }
                        return Mockito.RETURNS_DEFAULTS.answer(invocationOnMock);
                    }
                });
        try {
            final Channel mockChannel = mock(Channel.class, invocationOnMock -> {
                if ("queueDeclare".equals(invocationOnMock.getMethod().getName())) {
                    return mockDeclareOk;
                }
                return Mockito.RETURNS_DEFAULTS.answer(invocationOnMock);
            });
            when(mockConnection.isOpen()).thenReturn(true);
            when(mockConnection.createChannel()).thenReturn(mockChannel);
            when(mockConnection.createChannel(Mockito.anyInt())).thenReturn(mockChannel);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        return new AbstractConnectionFactory(mockConnectionFactory) {
            @Override
            public @NonNull org.springframework.amqp.rabbit.connection.Connection createConnection() {
                return super.createBareConnection();
            }
        };
    }
}

package dev.samsanders.demo.rabbitmq.publisher;

import org.apache.qpid.server.SystemLauncher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class EmbeddedAmqpBroker {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddedAmqpBroker.class);

    private final SystemLauncher systemLauncher;
    private final Map<String, Object> systemConfigurationAttributes;

    public EmbeddedAmqpBroker(String brokerConfigFileLocation) {
        this.systemLauncher = new SystemLauncher();
        this.systemConfigurationAttributes = new HashMap<>();
        this.systemConfigurationAttributes.put("type", "Memory");
        this.systemConfigurationAttributes.put("startupLoggedToSystemOut", true);

        URL initialConfiguration = EmbeddedAmqpBroker.class.getClassLoader().getResource(brokerConfigFileLocation);
        this.systemConfigurationAttributes.put("initialConfigurationLocation", initialConfiguration.toExternalForm());
    }

    public void start() {
        try {
            this.systemLauncher.startup(systemConfigurationAttributes);
        } catch (Exception e) {
            logger.error("Exception caught!", e);
            this.systemLauncher.shutdown(1);
        }
    }

    public void stop() {
        this.systemLauncher.shutdown();
    }

}

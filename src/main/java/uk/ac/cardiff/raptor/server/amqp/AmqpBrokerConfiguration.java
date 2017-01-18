package uk.ac.cardiff.raptor.server.amqp;

import org.apache.qpid.server.Broker;
import org.apache.qpid.server.BrokerOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.io.Files;

@Configuration
public class AmqpBrokerConfiguration {

	private static final Logger log = LoggerFactory.getLogger(AmqpBrokerConfiguration.class);

	@Bean
	public String apacheQpid() throws Exception {
		log.info("Setting up apache qPid");

		final int BROKER_PORT = 5672;

		final BrokerOptions brokerOptions = new BrokerOptions();
		final String configFileName = "/qpid-config.json";

		brokerOptions.setConfigProperty("broker.name", "embedded-broker");
		brokerOptions.setConfigProperty("qpid.amqp_port", String.valueOf(BROKER_PORT));

		brokerOptions.setConfigProperty("qpid.work_dir", Files.createTempDir().getAbsolutePath());
		brokerOptions.setInitialConfigurationLocation(getClass().getResource(configFileName).toString());

		final Broker broker = new Broker();

		broker.startup(brokerOptions);

		return "Test";

	}

}

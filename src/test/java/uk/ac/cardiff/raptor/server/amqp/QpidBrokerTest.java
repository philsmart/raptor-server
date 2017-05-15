package uk.ac.cardiff.raptor.server.amqp;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Optional;

import org.apache.qpid.server.Broker;
import org.apache.qpid.server.BrokerOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QpidBrokerTest {

	private static final Logger log = LoggerFactory.getLogger(QpidBrokerTest.class);

	private static final String INITIAL_CONFIG_PATH = "/Users/philsmart/Documents/Java/message-queues/qpid/config.json";

	public static final String PORT = "5675";

	private final Broker broker = new Broker();

	private Optional<String> getPathOfDirectory(final String directory) {
		final ClassLoader cl = ClassLoader.getSystemClassLoader();

		final URL[] urls = ((URLClassLoader) cl).getURLs();

		for (final URL url : urls) {
			final File dir = new File(url.getFile());
			if (dir.exists() && dir.isDirectory()) {
				log.debug("Listing files for {}", dir);
				for (final File asFile : dir.listFiles()) {
					log.debug("File {}", asFile.getName());
					if (asFile != null && asFile.exists()) {
						if (asFile.getName().equals(directory)) {
							return Optional.of(asFile.getAbsolutePath());
						}
					}
				}

			}

		}

		return Optional.empty();
	}

	public boolean startBroker() throws Exception {

		final Optional<String> path = getPathOfDirectory("qpid");
		log.debug("Path is {}", path);
		if (path.isPresent() == false) {
			return false;
		}

		final BrokerOptions brokerOptions = new BrokerOptions();
		brokerOptions.setConfigProperty("qpid.amqp_port", PORT);
		brokerOptions.setInitialConfigurationLocation(INITIAL_CONFIG_PATH);

		log.debug("Setting qpid work dir to {}", path.get());
		brokerOptions.setConfigProperty("qpid.work_dir", path.get());
		broker.startup(brokerOptions);
		log.info("Broker started {}");
		return true;
	}

	public void stopBroker() {
		broker.shutdown();
	}

}

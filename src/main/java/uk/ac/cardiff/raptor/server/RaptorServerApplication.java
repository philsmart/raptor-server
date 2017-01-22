package uk.ac.cardiff.raptor.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
@EntityScan("uk.ac.cardiff.model")
public class RaptorServerApplication {

	public static void main(final String[] args) throws InterruptedException {

		final ConfigurableApplicationContext ctx = new SpringApplication(RaptorServerApplication.class,
				"event-enrich-store.xml").run(args);

		ctx.registerShutdownHook();
		final Object lock = new Object();
		synchronized (lock) {
			lock.wait();
		}
		ctx.close();
	}
}

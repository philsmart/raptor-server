package uk.ac.cardiff.raptor.server.dao;

import java.util.Objects;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.repository.CrudRepository;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import uk.ac.cardiff.model.event.Event;

@Component
public class EventStore {

	private static final Logger log = LoggerFactory.getLogger(EventStore.class);

	/**
	 * The JPA event repository, specific version of a {@link CrudRepository}.
	 */
	@Inject
	private EventRepository repo;

	/**
	 * Validates the class setup
	 */
	@PostConstruct
	public void validate() {

		Objects.requireNonNull(repo, "EventStore requires an event repository");
	}

	/**
	 * Performs a {@link CrudRepository#save(Object)} operation to the input
	 * {@link Event}.
	 * 
	 * @param message
	 *            the message containing the {@link Event} to persist.
	 */
	@ServiceActivator
	public void storeEvent(final Message<Event> message) {
		log.info("Storing event [{}]", message.getPayload());
		final Event event = message.getPayload();
		repo.save(event);
		log.debug("Event stored");
	}

}

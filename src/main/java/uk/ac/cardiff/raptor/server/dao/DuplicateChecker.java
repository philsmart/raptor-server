package uk.ac.cardiff.raptor.server.dao;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.annotation.Filter;
import org.springframework.messaging.Message;

import uk.ac.cardiff.model.event.Event;

/**
 * Class that checks for duplicates using the {@link Event#getEventId().
 * 
 * @author philsmart
 *
 */
public class DuplicateChecker {

	private static final Logger log = LoggerFactory.getLogger(DuplicateChecker.class);

	@Inject
	private EventRepository repository;

	/**
	 * Checks if the {@link Event#getEventId()} contained in the payload of the
	 * {@link Message} is already present in the {@link EventRepository}. If it
	 * is, filters out the event, otherwise the event is allowed through
	 * 
	 * @param eventMsg
	 *            the {@link Message} with the {@link Event} in its payload
	 * @return true if the {@link Event} has not already been persisted, false
	 *         if it has.
	 */
	@Filter
	public boolean noDuplicates(final Message<Event> eventMsg) {

		if (eventMsg.getPayload() == null) {
			log.warn("Message has no payload, filtering this event out");
			return false;
		}
		log.info("Checking Event is not duplicated, eventId is [{}]", eventMsg.getPayload().getEventId());

		final Event match = repository.findOne(eventMsg.getPayload().getEventId());

		if (match == null) {
			log.debug("+ALLOW, No duplicate event found with eventId, allowing event [{}] at time [{}] to be persisted",
					eventMsg.getPayload().getEventId(), eventMsg.getPayload().getEventTime());
			return true;
		} else {
			log.debug("-DISALLOW, Found matching event, id = {}, and time = {}", eventMsg.getPayload().getEventId(),
					eventMsg.getPayload().getEventTime());
			return false;
		}

	}

}

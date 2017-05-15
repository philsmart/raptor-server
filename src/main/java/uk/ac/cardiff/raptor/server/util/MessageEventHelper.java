package uk.ac.cardiff.raptor.server.util;

import java.util.Optional;

import org.springframework.messaging.Message;

import uk.ac.cardiff.model.event.Event;

public class MessageEventHelper {

	/**
	 * Return the {@link Event} from a {@link Message} if it exists in the
	 * {@link Message#getPayload()}. Return an empty {@link Optional} otherwise.
	 * 
	 * @param msg
	 *            the {@link Message} to retrieve the {@link Event} from.
	 * @return either {@link Event} or empty {@link Optional}
	 */
	public static Optional<Event> getEventFromMessage(final Message<?> msg) {

		if (msg.getPayload() instanceof Event) {
			return Optional.of((Event) msg.getPayload());
		}
		return Optional.empty();

	}

}

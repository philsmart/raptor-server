package uk.ac.cardiff.raptor.server.amqp;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.core.MessageSelector;
import org.springframework.messaging.Message;

import uk.ac.cardiff.model.event.Event;
import uk.ac.cardiff.model.event.NullEvent;
import uk.ac.cardiff.raptor.server.error.ProcessingErrorConstants;
import uk.ac.cardiff.raptor.server.util.MessageEventHelper;

public class RetryEventFilter implements MessageSelector {

	private static final Logger log = LoggerFactory.getLogger(RetryEventFilter.class);

	/**
	 * The duration to wait before an event is retried again.
	 */
	@Value("${amqp.event.retry.retry-after:3600000}")
	private long retryAfter;

	@Override
	public boolean accept(final Message<?> message) {

		log.info("Filtering event message. Retry window will be respected");

		final Object retryTimestamp = message.getHeaders().get(ProcessingErrorConstants.RETRY_TIMESTAMP);

		final Optional<Event> eventOpt = MessageEventHelper.getEventFromMessage(message);

		if (retryTimestamp == null || retryTimestamp instanceof Long == false) {
			log.warn("Event [{}] does not have a retry timestamp or the timestamp is not a Long, silently discarding",
					eventOpt.orElse(new NullEvent()).getEventId());
		}
		final Long retryLong = (Long) retryTimestamp;

		final long timeSinceRetry = System.currentTimeMillis() - retryLong;

		log.debug("Retry timestamp for event [{}] is [{}], time since retry is [{}s] ",
				eventOpt.orElse(new NullEvent()).getEventId(), retryLong, timeSinceRetry / 1000f);

		if (timeSinceRetry >= retryAfter) {
			log.info("Event [{}] will be requeued", eventOpt.orElse(new NullEvent()).getEventId());
			return true;
		}

		return false;
	}

}

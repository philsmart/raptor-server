package uk.ac.cardiff.raptor.server.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.support.MessageBuilder;

import uk.ac.cardiff.model.event.Event;

/**
 * Service that processes any exception that occurred during Event processing.
 * Extracts the {@link Event} from the
 * {@link MessageHandlingException#getFailedMessage()}, adds a new
 * {@link ProcessingErrorConstants#ERROR_HEADER}. Increments or adds the
 * {@link ProcessingErrorConstants#RETRY_COUNT} in the headers. Constructs a new
 * {@link Message} of type {@link Event}, and sends it onward - typically for it
 * to be re-queued.
 * 
 * @author philsmart
 *
 */
public class EventProcessExceptionService {

	private static final Logger log = LoggerFactory.getLogger(EventProcessExceptionService.class);

	/**
	 * Name of the retry queue. For informational purpose.
	 */
	@Value("${amqp.event.queue.retry}")
	private String retryQueue;

	@ServiceActivator
	public Message<Event> handle(final Message<MessageHandlingException> eventMsg) {

		if (eventMsg.getPayload().getFailedMessage().getPayload() instanceof Event) {
			final Event event = (Event) eventMsg.getPayload().getFailedMessage().getPayload();

			log.error("Error has occured for event {}, with headers {}, error is", event,
					eventMsg.getPayload().getFailedMessage().getHeaders(), eventMsg.getPayload());
			log.info("Error event will be re-queued on the configured retry queue [{}]", retryQueue);

			int retryCount = 1;

			if (eventMsg.getPayload().getFailedMessage().getHeaders()
					.containsKey(ProcessingErrorConstants.RETRY_COUNT)) {

				final Object retryHeader = eventMsg.getPayload().getFailedMessage().getHeaders()
						.get(ProcessingErrorConstants.RETRY_COUNT);

				if (retryHeader instanceof Integer) {
					retryCount = (Integer) retryHeader;
					retryCount++;
				}

				log.debug("Retry header found in error message, has value {}, incrementing to {}", retryHeader,
						retryCount);
			}

			return MessageBuilder.withPayload(event).copyHeaders(eventMsg.getPayload().getFailedMessage().getHeaders())
					.setHeader(ProcessingErrorConstants.ERROR_HEADER, eventMsg.getPayload().getMessage())
					.setHeader(ProcessingErrorConstants.RETRY_COUNT, retryCount)
					.setHeader(ProcessingErrorConstants.RETRY_TIMESTAMP, System.currentTimeMillis()).build();

		} else {
			log.error(
					"Failed message was not an event, should not happened, silently ignoring message, was of type [{}]",
					eventMsg.getPayload().getFailedMessage().getPayload().getClass());

			return null;

		}

	}

}

package uk.ac.cardiff.raptor.server.enrich;

import java.util.Objects;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;

import uk.ac.cardiff.model.event.Event;

/**
 * The Spring Integration EventEnricher service that is autoconfigured on
 * startup to register the correct attribute.enrich.source e.g.
 * {@link LdapEventAttributeEnricher}.
 * 
 * @author philsmart
 *
 */
public class EventEnricherService {

	private static final Logger log = LoggerFactory.getLogger(EventEnricherService.class);

	private EventAttributeEnricher enricher;

	/**
	 * If true, an {@link EventAttributeEnricherException} is rethrow as a
	 * runtime exception to trigger rollback of containers transaction manager
	 * (if defined)
	 */
	private boolean exceptionTriggersRollbqck = false;

	public EventEnricherService(@Nonnull final EventAttributeEnricher ldapEnricher) {
		Objects.requireNonNull(ldapEnricher);
		enricher = ldapEnricher;
	}

	/**
	 * Enriches an event using the {@code enricher} configured.
	 * 
	 * @param eventMsg
	 *            the {@link Message} to enrich (in the payload)
	 * @return the enriched {@link Message}
	 */
	@ServiceActivator
	public Message<Event> enrich(final Message<Event> eventMsg) {
		if (eventMsg.getPayload() != null) {
			log.info("Enriching event {}", eventMsg.getPayload().getEventId());
			try {
				enricher.enrich(eventMsg.getPayload());
			} catch (final EventAttributeEnricherException e) {
				log.error("Could not enrich event [{}]", eventMsg.getPayload().getEventId(), e);
				if (exceptionTriggersRollbqck) {
					throw new EventAttributeEnricherRollbackException(e);
				}
			}
		}
		return eventMsg;

	}

	public EventAttributeEnricher getEnricher() {
		return enricher;
	}

	public void setEnricher(final EventAttributeEnricher enricher) {
		this.enricher = enricher;
	}

	/**
	 * @return the exceptionTriggersRollbqck
	 */
	public boolean isExceptionTriggersRollbqck() {
		return exceptionTriggersRollbqck;
	}

	/**
	 * @param exceptionTriggersRollbqck
	 *            the exceptionTriggersRollbqck to set
	 */
	public void setExceptionTriggersRollbqck(final boolean exceptionTriggersRollbqck) {
		this.exceptionTriggersRollbqck = exceptionTriggersRollbqck;
	}

}

package uk.ac.cardiff.raptor.server.enrich;

import java.util.Collections;
import java.util.List;
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

	private List<AbstractEventAttributeEnricher> enrichers;

	/**
	 * If true, an {@link EventAttributeEnricherException} is rethrow as a runtime
	 * exception to trigger rollback of containers transaction manager (if defined)
	 */
	private boolean exceptionTriggersRollbqck = false;

	public EventEnricherService(@Nonnull final List<AbstractEventAttributeEnricher> enrichers) {
		Objects.requireNonNull(enrichers);
		this.enrichers = enrichers;
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
			log.trace("Enriching event {}", eventMsg.getPayload().getEventId());
			try {
				boolean wasEnriched = false;
				for (final AbstractEventAttributeEnricher enricher : enrichers) {
					log.trace("Checking enricher for class {} is suitable for type {}, {}", enricher.getForClass(),
							eventMsg.getPayload().getClass(),
							enricher.getForClass() == eventMsg.getPayload().getClass());
					if (enricher.getForClass() == eventMsg.getPayload().getClass()) {
						enricher.enrich(eventMsg.getPayload());
						wasEnriched = true;
					}
				}
				log.debug("Was event [{}] *possibly* enriched by at least one enricher, {}",
						eventMsg.getPayload().getEventId(), wasEnriched ? "yes" : "no");
			} catch (final EventAttributeEnricherException e) {
				log.error("Exception thrown while trying to enrich Event [{}]", eventMsg.getPayload().getEventId(), e);
				if (exceptionTriggersRollbqck) {
					throw new EventAttributeEnricherRollbackException(e);
				}
			}
		}
		return eventMsg;

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

	/**
	 * @return the enrichers
	 */
	public List<AbstractEventAttributeEnricher> getEnrichers() {
		return Collections.unmodifiableList(enrichers);
	}

	public void setEnrichers(final List<AbstractEventAttributeEnricher> enrich) {
		enrichers = enrich;
	}

}

package uk.ac.cardiff.raptor.server.enrich;

import uk.ac.cardiff.model.event.Event;

/**
 * Interface to support adding attribute information (based on a single
 * principal name lookup) to an event. Should extend
 * {@link EventAttributeEnricher} rather than directly implement this interface
 * 
 * @author philsmart
 *
 */
public interface EventAttributeEnricher {

	/**
	 * Add additional attribute information to the input {@link Event}.
	 * 
	 * @param event
	 *            the {@link Event} to augment with information.
	 */
	void enrich(final Event event) throws EventAttributeEnricherException;

}

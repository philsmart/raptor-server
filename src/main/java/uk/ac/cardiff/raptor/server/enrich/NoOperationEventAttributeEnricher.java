package uk.ac.cardiff.raptor.server.enrich;

import uk.ac.cardiff.model.event.Event;

public class NoOperationEventAttributeEnricher extends AbstractEventAttributeEnricher {

	@Override
	public void enrich(final Event event) throws EventAttributeEnricherException {
		// do nothing.

	}

}

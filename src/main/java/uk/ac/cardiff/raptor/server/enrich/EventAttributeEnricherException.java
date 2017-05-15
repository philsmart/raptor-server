package uk.ac.cardiff.raptor.server.enrich;

public class EventAttributeEnricherException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9154739315093807950L;

	public EventAttributeEnricherException() {
		super();

	}

	public EventAttributeEnricherException(final String message, final Throwable cause, final boolean enableSuppression,
			final boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);

	}

	public EventAttributeEnricherException(final String message, final Throwable cause) {
		super(message, cause);

	}

	public EventAttributeEnricherException(final String message) {
		super(message);

	}

	public EventAttributeEnricherException(final Throwable cause) {
		super(cause);

	}

}

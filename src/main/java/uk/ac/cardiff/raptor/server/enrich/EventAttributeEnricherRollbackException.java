package uk.ac.cardiff.raptor.server.enrich;

public class EventAttributeEnricherRollbackException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9154739315093807950L;

	public EventAttributeEnricherRollbackException() {
		super();

	}

	public EventAttributeEnricherRollbackException(final String message, final Throwable cause,
			final boolean enableSuppression, final boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);

	}

	public EventAttributeEnricherRollbackException(final String message, final Throwable cause) {
		super(message, cause);

	}

	public EventAttributeEnricherRollbackException(final String message) {
		super(message);

	}

	public EventAttributeEnricherRollbackException(final Throwable cause) {
		super(cause);

	}

}

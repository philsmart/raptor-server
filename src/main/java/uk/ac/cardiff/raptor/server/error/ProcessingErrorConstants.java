package uk.ac.cardiff.raptor.server.error;

public class ProcessingErrorConstants {

	/**
	 * A description of the last error that caused the message to be placed on
	 * the retry queue.
	 */
	public static final String ERROR_HEADER = "x_raptor_error";

	/**
	 * How many times the message has been put on the retry queue. The first
	 * time starting with 1.
	 */
	public static final String RETRY_COUNT = "x_raptor_retry_count";

	/**
	 * The time in ms since unix EPOCH that the message was placed on the retry
	 * queue.
	 */
	public static final String RETRY_TIMESTAMP = "x_raptor_retry_timestamp";

}

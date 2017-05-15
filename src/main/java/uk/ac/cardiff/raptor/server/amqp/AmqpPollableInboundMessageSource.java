package uk.ac.cardiff.raptor.server.amqp;

import java.util.Map;
import java.util.Objects;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.integration.amqp.support.AmqpHeaderMapper;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.integration.core.MessageSource;
import org.springframework.messaging.support.MessageBuilder;

import uk.ac.cardiff.model.event.Event;

/**
 * A custom pollable AMQP message source.
 * 
 * @author philsmart
 *
 */
public class AmqpPollableInboundMessageSource implements MessageSource<Event> {

	private static final Logger log = LoggerFactory.getLogger(AmqpPollableInboundMessageSource.class);

	private AmqpTemplate amqp;

	private volatile AmqpHeaderMapper headerMapper = DefaultAmqpHeaderMapper.inboundMapper();

	private volatile MessageConverter amqpMessageConverter = new SimpleMessageConverter();

	private String amqpQueue;

	@PostConstruct
	public void init() {

		Objects.requireNonNull(amqp, "Requires an AMQP Template");
		Objects.requireNonNull(amqpQueue, "Requires an AMQP Queue Name");
		log.info("Initialising AMQP Polling Inbound channel, for queue [{}]", amqpQueue);

	}

	@Override
	public org.springframework.messaging.Message<Event> receive() {
		log.trace("Polling Amqp Queue");

		final Message message = amqp.receive(amqpQueue);

		if (message != null) {

			log.debug("AMQP raw message inbound [{}]", message);

			final Object messageObject = amqpMessageConverter.fromMessage(message);

			final Map<String, Object> headers = headerMapper.toHeadersFromReply(message.getMessageProperties());

			log.debug("AMQP Inbound has recieved message[{},{}]", messageObject, headers);

			if (messageObject instanceof Event) {
				return MessageBuilder.withPayload((Event) messageObject).copyHeaders(headers).build();
			}
		}

		return null;
	}

	/**
	 * @return the amqp
	 */
	public AmqpTemplate getAmqp() {
		return amqp;
	}

	/**
	 * @param amqp
	 *            the amqp to set
	 */
	public void setAmqp(final AmqpTemplate amqp) {
		this.amqp = amqp;
	}

	/**
	 * @return the amqpQueue
	 */
	public String getAmqpQueue() {
		return amqpQueue;
	}

	/**
	 * @param amqpQueue
	 *            the amqpQueue to set
	 */
	public void setAmqpQueue(final String amqpQueue) {
		this.amqpQueue = amqpQueue;
	}

	/**
	 * @return the amqpMessageConverter
	 */
	public MessageConverter getAmqpMessageConverter() {
		return amqpMessageConverter;
	}

	/**
	 * @param amqpMessageConverter
	 *            the amqpMessageConverter to set
	 */
	public void setAmqpMessageConverter(final MessageConverter amqpMessageConverter) {
		this.amqpMessageConverter = amqpMessageConverter;
	}

}

package uk.ac.cardiff.raptor.server;

import javax.inject.Inject;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.cardiff.model.event.Event;
import uk.ac.cardiff.raptor.server.amqp.QpidBrokerTest;
import uk.ac.cardiff.raptor.server.dao.EventRepository;
import uk.ac.cardiff.raptor.server.enrich.EventEnricherService;
import uk.ac.cardiff.raptor.server.error.ProcessingErrorConstants;

@TestPropertySource(locations = "/application-test.properties", properties = { "amqp.event.start=false",
		"amqp.event.retry.start=true", "amqp.event.retry.retry-after=1" })
public class AmqpRetryQueueTest extends BaseServerTest {

	private static final Logger log = LoggerFactory.getLogger(AmqpRetryQueueTest.class);

	private static QpidBrokerTest broker;

	@Inject
	private AmqpTemplate ampqTemplate;

	@Inject
	private EventRepository eventRepo;

	@Inject
	private EventEnricherService enricher;

	@BeforeClass
	public static void startup() throws Exception {
		broker = new QpidBrokerTest();
		broker.startBroker();

	}

	@AfterClass
	public static void tearDown() throws Exception {
		broker.stopBroker();
	}

	@Test
	public void testAmqpRetryQueuePoller() throws Exception {

		enricher.setExceptionTriggersRollbqck(true);

		final Event mockEvent = mockShibEvent("scmps2");

		final ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new com.fasterxml.jackson.datatype.joda.JodaModule());
		final String json = mapper.writeValueAsString(mockEvent);
		log.debug("Constructed JSON {}", json);

		final org.springframework.amqp.core.MessageProperties props = new org.springframework.amqp.core.MessageProperties();
		props.setHeader(ProcessingErrorConstants.RETRY_COUNT, 1);
		props.setHeader(ProcessingErrorConstants.ERROR_HEADER, "no real error");
		props.setHeader(ProcessingErrorConstants.RETRY_TIMESTAMP, System.currentTimeMillis());
		props.setContentType("application/json");
		props.setHeader("__TypeId__", "uk.ac.cardiff.model.event.ShibbolethIdpAuthenticationEvent");

		final Message forQueue = new Message(json.getBytes(), props);

		ampqTemplate.send("raptor.harvest.test-retry", forQueue);

		final Message recMsg = ampqTemplate.receive("raptor.harvest.test", 2000);

		final Event fromInputQueue = convertShibbolethIdpAuthenticationEvent(recMsg);

		log.info("Has message from import queue {}", recMsg);

		org.junit.Assert.assertEquals("Event ID of retry event is not the same as the input Event",
				mockEvent.getEventId(), fromInputQueue.getEventId());

		Assert.notNull(recMsg);

	}

}

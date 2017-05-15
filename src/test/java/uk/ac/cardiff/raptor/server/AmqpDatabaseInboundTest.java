package uk.ac.cardiff.raptor.server;

import javax.inject.Inject;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.test.context.TestPropertySource;

import uk.ac.cardiff.model.event.Event;
import uk.ac.cardiff.raptor.server.amqp.QpidBrokerTest;
import uk.ac.cardiff.raptor.server.dao.EventRepository;
import uk.ac.cardiff.raptor.server.enrich.EventEnricherService;

@TestPropertySource(locations = "/application-test.properties", properties = { "amqp.event.start=true",
		"amqp.event.retry.start=false" })
public class AmqpDatabaseInboundTest extends BaseServerTest {

	private static final Logger log = LoggerFactory.getLogger(AmqpDatabaseInboundTest.class);

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
	public void testDbUnavailableRollback() throws Exception {

		enricher.setExceptionTriggersRollbqck(false);

		final Event mockEvent = mockShibEventLongResourceId("scmps2");

		ampqTemplate.convertAndSend("raptor.harvest.test", mockEvent);

		final long records = eventRepo.count();

		log.info("Has {} results in db, should be 0", records);

		Assert.assertTrue(records == 0);

		final Message recMsg = ampqTemplate.receive("raptor.harvest.test-retry", 5000);

		Assert.assertNotNull("Expected event on retry queue", recMsg);

		compareEvent(recMsg, mockEvent, 1);

	}

	@Test
	public void testRetryCountIncrementOnDbError() {

		enricher.setExceptionTriggersRollbqck(false);

		final Event mockEvent = mockShibEventLongResourceId("scmps2");

		ampqTemplate.convertAndSend("raptor.harvest.test", mockEvent);

		final long records = eventRepo.count();

		log.info("Has {} results in db, should be 0", records);

		Assert.assertTrue(records == 0);

		Message recMsg = ampqTemplate.receive("raptor.harvest.test-retry", 5000);

		Assert.assertNotNull("Expected event on retry queue", recMsg);

		final Event retryEvent = compareEvent(recMsg, mockEvent, 1);

		ampqTemplate.send("raptor.harvest.test", recMsg);

		recMsg = ampqTemplate.receive("raptor.harvest.test-retry", 5000);

		compareEvent(recMsg, mockEvent, 2);

	}

}

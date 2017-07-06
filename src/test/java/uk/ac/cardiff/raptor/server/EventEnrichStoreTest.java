package uk.ac.cardiff.raptor.server;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.context.TestPropertySource;

import uk.ac.cardiff.model.event.AuthenticationEvent;
import uk.ac.cardiff.model.event.Event;
import uk.ac.cardiff.model.event.ShibbolethIdpAuthenticationEvent;
import uk.ac.cardiff.raptor.server.dao.EventRepository;
import uk.ac.cardiff.raptor.server.enrich.EventEnricherService;

@TestPropertySource(locations = "/application-test.properties", properties = { "amqp.event.start=false",
		"amqp.event.retry.start=false" })
public class EventEnrichStoreTest extends BaseServerTest {

	private static final Logger log = LoggerFactory.getLogger(EventEnrichStoreTest.class);

	@Inject
	MessageChannel amqpEventChnl;

	@Inject
	private EventRepository repo;

	@Inject
	private EventEnricherService enricher;

	@Test
	public void checkDuplicates() {
		log.info("Checking duplicates are not created");
		enricher.setExceptionTriggersRollbqck(false);

		repo.deleteAll();

		final Event mockEvent = mockEventFixedId("usernameone");
		final Event mockEventTwo = mockEventFixedId("usernametwo");
		log.debug("Has message channel {}", amqpEventChnl);
		final Message<Event> pushEvent = MessageBuilder.withPayload(mockEvent).build();
		final Message<Event> pushEventTwo = MessageBuilder.withPayload(mockEventTwo).build();

		Assert.assertTrue(pushEvent.getPayload().getEventId() == pushEventTwo.getPayload().getEventId());

		Assert.assertTrue(amqpEventChnl.send(pushEvent));
		Assert.assertTrue(amqpEventChnl.send(pushEventTwo));

		final long numberInRepository = repo.count();
		log.info("** Has {} events in repository", numberInRepository);
		Assert.assertEquals(1, numberInRepository);
		printTable();
	}

	@Test
	public void addEzproxyRecord() {
		log.info("Adding 1 ezproxy record for full enrich and store");
		enricher.setExceptionTriggersRollbqck(false);

		repo.deleteAll();

		final Event mockEvent = mockEzproxyEvent("ezproxy-match");

		final Message<Event> pushEvent = MessageBuilder.withPayload(mockEvent).build();

		Assert.assertTrue(amqpEventChnl.send(pushEvent));

		final Event mockEventFromRepo = repo.findOne(mockEvent.getEventId());
		Assert.assertNotNull(mockEventFromRepo);
		log.debug("Has EZproxy Mock Event One From Repo as {}", mockEventFromRepo);
		Assert.assertTrue(mockEventFromRepo instanceof AuthenticationEvent);
		Assert.assertNotNull(((AuthenticationEvent) mockEventFromRepo).getPrincipalInformation());
		Assert.assertNotNull(((AuthenticationEvent) mockEventFromRepo).getPrincipalInformation().getAffiliation());
		Assert.assertNotNull(((AuthenticationEvent) mockEventFromRepo).getPrincipalInformation().getSchool());
		Assert.assertTrue(
				((AuthenticationEvent) mockEventFromRepo).getPrincipalInformation().getAffiliation().equals("R"));
		Assert.assertTrue(((AuthenticationEvent) mockEventFromRepo).getPrincipalInformation().getSchool()
				.equals("ezproxyTestSchool"));

		final long numberInRepository = repo.count();
		log.info("** Has {} events in repository", numberInRepository);
		Assert.assertEquals(1, numberInRepository);

		printTable();
	}

	@Test
	public void addThreeShibRecords() {
		log.info("Checking 3 records are added");
		enricher.setExceptionTriggersRollbqck(false);

		repo.deleteAll();

		final Event mockEvent = mockShibEvent("usernameone");
		final Event mockEventTwo = mockShibEvent("usernametwo");
		final Event mockEventThree = mockShibEvent("username-not-found");
		log.debug("Has message channel {}", amqpEventChnl);
		final Message<Event> pushEvent = MessageBuilder.withPayload(mockEvent).build();
		final Message<Event> pushEventTwo = MessageBuilder.withPayload(mockEventTwo).build();
		final Message<Event> pushEventThree = MessageBuilder.withPayload(mockEventThree).build();

		Assert.assertTrue(amqpEventChnl.send(pushEvent));
		Assert.assertTrue(amqpEventChnl.send(pushEventTwo));
		Assert.assertTrue(amqpEventChnl.send(pushEventThree));

		final long numberInRepository = repo.count();
		log.info("** Has {} events in repository", numberInRepository);
		Assert.assertEquals(3, numberInRepository);

		final Event mockEventFromRepo = repo.findOne(mockEvent.getEventId());
		Assert.assertNotNull(mockEventFromRepo);
		log.debug("Has Mock Event One From Repo as {}", mockEventFromRepo);
		Assert.assertTrue(mockEventFromRepo instanceof AuthenticationEvent);
		Assert.assertNotNull(((AuthenticationEvent) mockEventFromRepo).getPrincipalInformation());
		Assert.assertNotNull(((AuthenticationEvent) mockEventFromRepo).getPrincipalInformation().getAffiliation());
		Assert.assertNotNull(((AuthenticationEvent) mockEventFromRepo).getPrincipalInformation().getSchool());
		Assert.assertTrue(
				((AuthenticationEvent) mockEventFromRepo).getPrincipalInformation().getAffiliation().equals("P"));
		Assert.assertTrue(
				((AuthenticationEvent) mockEventFromRepo).getPrincipalInformation().getSchool().equals("schoolOne"));

		final Event mockEventTwoFromRepo = repo.findOne(mockEventTwo.getEventId());
		Assert.assertNotNull(mockEventTwoFromRepo);
		log.debug("Has Mock Event Two From Repo as {}", mockEventTwoFromRepo);
		Assert.assertTrue(mockEventTwoFromRepo instanceof AuthenticationEvent);
		Assert.assertNotNull(((AuthenticationEvent) mockEventTwoFromRepo).getPrincipalInformation());
		Assert.assertNotNull(((AuthenticationEvent) mockEventTwoFromRepo).getPrincipalInformation().getAffiliation());
		Assert.assertNotNull(((AuthenticationEvent) mockEventTwoFromRepo).getPrincipalInformation().getSchool());
		Assert.assertTrue(
				((AuthenticationEvent) mockEventTwoFromRepo).getPrincipalInformation().getAffiliation().equals("R"));
		Assert.assertTrue(
				((AuthenticationEvent) mockEventTwoFromRepo).getPrincipalInformation().getSchool().equals("schoolTwo"));

		printTable();
	}

	/**
	 * As the eventid of an empty event is 0, only one empty event should ever
	 * be persisted.
	 */
	@Test
	public void testEmptyEvent() {
		enricher.setExceptionTriggersRollbqck(false);

		repo.deleteAll();

		final ShibbolethIdpAuthenticationEvent event = new ShibbolethIdpAuthenticationEvent();
		final ShibbolethIdpAuthenticationEvent eventTwo = new ShibbolethIdpAuthenticationEvent();

		final Message<ShibbolethIdpAuthenticationEvent> pushEvent = MessageBuilder.withPayload(event).build();
		Assert.assertTrue(amqpEventChnl.send(pushEvent));

		final Message<ShibbolethIdpAuthenticationEvent> pushEventTwo = MessageBuilder.withPayload(eventTwo).build();
		Assert.assertTrue(amqpEventChnl.send(pushEventTwo));

		Assert.assertEquals(1, repo.count());

	}

	private void printTable() {
		int count = 1;
		for (final Event event : repo.findAll()) {
			log.info("SAVED {}: {}", count++, event);
		}

	}

}

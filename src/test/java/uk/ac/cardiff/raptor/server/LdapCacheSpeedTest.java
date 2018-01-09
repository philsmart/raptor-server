package uk.ac.cardiff.raptor.server;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.StopWatch;

import uk.ac.cardiff.model.event.AuthenticationEvent;
import uk.ac.cardiff.model.event.Event;
import uk.ac.cardiff.raptor.server.dao.EventRepository;
import uk.ac.cardiff.raptor.server.enrich.EventEnricherService;
import uk.ac.cardiff.raptor.server.enrich.LdapEventAttributeEnricher;

@TestPropertySource(locations = "/application-test.properties", properties = { "amqp.event.start=false",
		"amqp.event.retry.start=false" })
public class LdapCacheSpeedTest extends BaseServerTest {

	private static final Logger log = LoggerFactory.getLogger(EventEnrichStoreTest.class);

	@Inject
	private EventEnricherService enricher;

	@Inject
	MessageChannel amqpEventChnl;

	@Inject
	EventEnricherService enricherService;

	@Inject
	LdapTemplate ldap;

	@Inject
	private EventRepository repo;

	/**
	 * <p>
	 * this tests FINISH
	 * </p>
	 * 
	 * 
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void ldapEnrichmentShibEventCacheTest() throws InterruptedException {

		enricher.setExceptionTriggersRollbqck(false);

		// set the cache and timeout on any LdapEventAttributeEnricher
		enricherService.getEnrichers().forEach(enrich -> {
			if (enrich instanceof LdapEventAttributeEnricher) {
				((LdapEventAttributeEnricher) enrich).setCacheExpiryAfterWriteMs(1000);
				((LdapEventAttributeEnricher) enrich).setUseCache(true);
				((LdapEventAttributeEnricher) enrich).init();
			}
		});

		repo.deleteAll();

		final List<Long> times = new ArrayList<Long>();

		final Event mockEvent = mockEventFixedId("usernameone");

		log.debug("Has message channel {}", amqpEventChnl);
		final Message<Event> pushEvent = MessageBuilder.withPayload(mockEvent).build();

		final StopWatch timer = new StopWatch("Time Complete Event Enrich Store");
		timer.start();
		Assert.assertTrue(amqpEventChnl.send(pushEvent));
		timer.stop();
		times.add(timer.getLastTaskTimeMillis());
		Assert.assertTrue(repo.count() == 1);
		Event mockEventFromRepo = repo.findOne(mockEvent.getEventId());
		Assert.assertNotNull(mockEventFromRepo);
		log.debug("Has Mock Event From Repo {}", mockEventFromRepo);
		Assert.assertTrue(mockEventFromRepo instanceof AuthenticationEvent);
		Assert.assertNotNull(((AuthenticationEvent) mockEventFromRepo).getPrincipalInformation());
		Assert.assertEquals("P", ((AuthenticationEvent) mockEventFromRepo).getPrincipalInformation().getAffiliation());
		Assert.assertEquals("schoolOne",
				((AuthenticationEvent) mockEventFromRepo).getPrincipalInformation().getSchool());
		printTable();
		repo.deleteAll();
		log.info("*** Event Enrich Store has taken [{}] ms", timer.getLastTaskTimeMillis());

		// this should use old cached values - so same as above.
		ldap.modifyAttributes("cn=usernameone,cn=A041991C,o=people", createModify("businessCategory", "U"));
		ldap.modifyAttributes("cn=usernameone,cn=A041991C,o=people", createModify("description", "schoolTmp"));
		timer.start();
		Assert.assertTrue(amqpEventChnl.send(pushEvent));
		timer.stop();
		times.add(timer.getLastTaskTimeMillis());
		Assert.assertTrue(repo.count() == 1);
		mockEventFromRepo = repo.findOne(mockEvent.getEventId());
		Assert.assertNotNull(mockEventFromRepo);
		log.debug("Has Mock Event From Repo {}", mockEventFromRepo);
		Assert.assertTrue(mockEventFromRepo instanceof AuthenticationEvent);
		Assert.assertNotNull(((AuthenticationEvent) mockEventFromRepo).getPrincipalInformation());
		Assert.assertEquals("P", ((AuthenticationEvent) mockEventFromRepo).getPrincipalInformation().getAffiliation());
		Assert.assertEquals("schoolOne",
				((AuthenticationEvent) mockEventFromRepo).getPrincipalInformation().getSchool());
		printTable();
		repo.deleteAll();
		log.info("*** Event Enrich Store Second (with cache) has taken [{}] ms", timer.getLastTaskTimeMillis());

		// cache should have expired, so should use the new values
		Thread.sleep(2000);

		timer.start();
		Assert.assertTrue(amqpEventChnl.send(pushEvent));
		timer.stop();
		times.add(timer.getLastTaskTimeMillis());
		Assert.assertTrue(repo.count() == 1);
		mockEventFromRepo = repo.findOne(mockEvent.getEventId());
		Assert.assertNotNull(mockEventFromRepo);
		log.debug("Has Mock Event From Repo {}", mockEventFromRepo);
		Assert.assertTrue(mockEventFromRepo instanceof AuthenticationEvent);
		Assert.assertNotNull(((AuthenticationEvent) mockEventFromRepo).getPrincipalInformation());
		Assert.assertEquals("U", ((AuthenticationEvent) mockEventFromRepo).getPrincipalInformation().getAffiliation());
		Assert.assertEquals("schoolTmp",
				((AuthenticationEvent) mockEventFromRepo).getPrincipalInformation().getSchool());
		printTable();
		repo.deleteAll();
		log.info("*** Event Enrich Store Third (cache timeout) has taken [{}] ms", timer.getLastTaskTimeMillis());

		times.forEach(t -> log.info("--Execution Time [{}]", t));

		// reset the ldap values for other tests

		ldap.modifyAttributes("cn=usernameone,cn=A041991C,o=people", createModify("businessCategory", "P"));
		ldap.modifyAttributes("cn=usernameone,cn=A041991C,o=people", createModify("description", "schoolOne"));

	}

	@Test
	public void ldapEnrichmentEzproxyEventCacheTest() throws InterruptedException {

		enricher.setExceptionTriggersRollbqck(false);

		// set the cache and timeout on any LdapEventAttributeEnricher
		enricherService.getEnrichers().forEach(enrich -> {
			if (enrich instanceof LdapEventAttributeEnricher) {
				((LdapEventAttributeEnricher) enrich).setCacheExpiryAfterWriteMs(1000);
				((LdapEventAttributeEnricher) enrich).setUseCache(true);
				((LdapEventAttributeEnricher) enrich).init();
			}
		});

		repo.deleteAll();

		final List<Long> times = new ArrayList<Long>();

		final Event mockEvent = mockEzproxyEvent("ezproxy-match");

		log.debug("Has message channel {}", amqpEventChnl);
		final Message<Event> pushEvent = MessageBuilder.withPayload(mockEvent).build();

		final StopWatch timer = new StopWatch("Time Complete Event Enrich Store");
		timer.start();
		Assert.assertTrue(amqpEventChnl.send(pushEvent));
		timer.stop();
		times.add(timer.getLastTaskTimeMillis());
		Assert.assertTrue(repo.count() == 1);
		Event mockEventFromRepo = repo.findOne(mockEvent.getEventId());
		Assert.assertNotNull(mockEventFromRepo);
		log.debug("Has Mock Event From Repo {}", mockEventFromRepo);
		Assert.assertTrue(mockEventFromRepo instanceof AuthenticationEvent);
		Assert.assertNotNull(((AuthenticationEvent) mockEventFromRepo).getPrincipalInformation());
		Assert.assertEquals("R", ((AuthenticationEvent) mockEventFromRepo).getPrincipalInformation().getAffiliation());
		Assert.assertEquals("ezproxyTestSchool",
				((AuthenticationEvent) mockEventFromRepo).getPrincipalInformation().getSchool());
		printTable();
		repo.deleteAll();
		log.info("*** Event Enrich Store has taken [{}] ms", timer.getLastTaskTimeMillis());

		// this should use old cached values - so same as above.
		ldap.modifyAttributes("cn=ezproxyuser,cn=A049000,o=people", createModify("businessCategory", "U"));
		ldap.modifyAttributes("cn=ezproxyuser,cn=A049000,o=people", createModify("description", "schoolTmp"));
		timer.start();
		Assert.assertTrue(amqpEventChnl.send(pushEvent));
		timer.stop();
		times.add(timer.getLastTaskTimeMillis());
		Assert.assertTrue(repo.count() == 1);
		mockEventFromRepo = repo.findOne(mockEvent.getEventId());
		Assert.assertNotNull(mockEventFromRepo);
		log.debug("Has Mock Event From Repo {}", mockEventFromRepo);
		Assert.assertTrue(mockEventFromRepo instanceof AuthenticationEvent);
		Assert.assertNotNull(((AuthenticationEvent) mockEventFromRepo).getPrincipalInformation());
		Assert.assertEquals("R", ((AuthenticationEvent) mockEventFromRepo).getPrincipalInformation().getAffiliation());
		Assert.assertEquals("ezproxyTestSchool",
				((AuthenticationEvent) mockEventFromRepo).getPrincipalInformation().getSchool());
		printTable();
		repo.deleteAll();
		log.info("*** Event Enrich Store Second (with cache) has taken [{}] ms", timer.getLastTaskTimeMillis());

		// cache should have expired, so should use the new values
		Thread.sleep(2000);

		timer.start();
		Assert.assertTrue(amqpEventChnl.send(pushEvent));
		timer.stop();
		times.add(timer.getLastTaskTimeMillis());
		Assert.assertTrue(repo.count() == 1);
		mockEventFromRepo = repo.findOne(mockEvent.getEventId());
		Assert.assertNotNull(mockEventFromRepo);
		log.debug("Has Mock Event From Repo {}", mockEventFromRepo);
		Assert.assertTrue(mockEventFromRepo instanceof AuthenticationEvent);
		Assert.assertNotNull(((AuthenticationEvent) mockEventFromRepo).getPrincipalInformation());
		Assert.assertEquals("U", ((AuthenticationEvent) mockEventFromRepo).getPrincipalInformation().getAffiliation());
		Assert.assertEquals("schoolTmp",
				((AuthenticationEvent) mockEventFromRepo).getPrincipalInformation().getSchool());
		printTable();
		repo.deleteAll();
		log.info("*** Event Enrich Store Third (cache timeout) has taken [{}] ms", timer.getLastTaskTimeMillis());

		times.forEach(t -> log.info("--Execution Time [{}]", t));

		// reset the ldap values for other tests

		ldap.modifyAttributes("cn=ezproxyuser,cn=A049000,o=people", createModify("businessCategory", "R"));
		ldap.modifyAttributes("cn=ezproxyuser,cn=A049000,o=people", createModify("description", "ezproxyTestSchool"));

	}

	@Test
	public void ldapEnrichmentShibEventDoNotUseCacheTest() throws InterruptedException {

		enricher.setExceptionTriggersRollbqck(false);

		// set the cache and timeout on any LdapEventAttributeEnricher
		enricherService.getEnrichers().forEach(enrich -> {
			if (enrich instanceof LdapEventAttributeEnricher) {
				((LdapEventAttributeEnricher) enrich).setCacheExpiryAfterWriteMs(1000);
				((LdapEventAttributeEnricher) enrich).setUseCache(false);
				((LdapEventAttributeEnricher) enrich).init();
			}
		});

		repo.deleteAll();

		final List<Long> times = new ArrayList<Long>();

		final Event mockEvent = mockEventFixedId("usernameone");

		log.debug("Has message channel {}", amqpEventChnl);
		final Message<Event> pushEvent = MessageBuilder.withPayload(mockEvent).build();

		Assert.assertTrue(amqpEventChnl.send(pushEvent));

		Assert.assertTrue(repo.count() == 1);
		Event mockEventFromRepo = repo.findOne(mockEvent.getEventId());
		Assert.assertNotNull(mockEventFromRepo);
		log.debug("Has Mock Event From Repo {}", mockEventFromRepo);
		Assert.assertTrue(mockEventFromRepo instanceof AuthenticationEvent);
		Assert.assertNotNull(((AuthenticationEvent) mockEventFromRepo).getPrincipalInformation());
		Assert.assertEquals("P", ((AuthenticationEvent) mockEventFromRepo).getPrincipalInformation().getAffiliation());
		Assert.assertEquals("schoolOne",
				((AuthenticationEvent) mockEventFromRepo).getPrincipalInformation().getSchool());
		printTable();
		repo.deleteAll();

		// this should use new values as no cache
		ldap.modifyAttributes("cn=usernameone,cn=A041991C,o=people", createModify("businessCategory", "U"));
		ldap.modifyAttributes("cn=usernameone,cn=A041991C,o=people", createModify("description", "schoolTmp"));

		Assert.assertTrue(amqpEventChnl.send(pushEvent));

		Assert.assertTrue(repo.count() == 1);
		mockEventFromRepo = repo.findOne(mockEvent.getEventId());
		Assert.assertNotNull(mockEventFromRepo);
		log.debug("Has Mock Event From Repo {}", mockEventFromRepo);
		Assert.assertTrue(mockEventFromRepo instanceof AuthenticationEvent);
		Assert.assertNotNull(((AuthenticationEvent) mockEventFromRepo).getPrincipalInformation());
		Assert.assertEquals("U", ((AuthenticationEvent) mockEventFromRepo).getPrincipalInformation().getAffiliation());
		Assert.assertEquals("schoolTmp",
				((AuthenticationEvent) mockEventFromRepo).getPrincipalInformation().getSchool());
		printTable();
		repo.deleteAll();

		// reset the ldap values for other tests

		ldap.modifyAttributes("cn=usernameone,cn=A041991C,o=people", createModify("businessCategory", "P"));
		ldap.modifyAttributes("cn=usernameone,cn=A041991C,o=people", createModify("description", "schoolOne"));

	}

	private ModificationItem[] createModify(final String attribute, final String value) {

		final ModificationItem item = new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
				new BasicAttribute(attribute, value));

		return new ModificationItem[] { item };
	}

	private void printTable() {
		int count = 1;
		for (final Event event : repo.findAll()) {
			log.info("SAVED {}: {}", count++, event);
		}

	}

}

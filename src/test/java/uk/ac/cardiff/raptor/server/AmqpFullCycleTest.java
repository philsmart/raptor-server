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
import org.springframework.integration.amqp.inbound.AmqpInboundChannelAdapter;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.test.context.TestPropertySource;

import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.cardiff.model.event.Event;
import uk.ac.cardiff.raptor.server.amqp.QpidBrokerTest;
import uk.ac.cardiff.raptor.server.dao.EventRepository;
import uk.ac.cardiff.raptor.server.enrich.EventEnricherService;
import uk.ac.cardiff.raptor.server.enrich.LdapEventAttributeEnricher;

@TestPropertySource(locations = "/application-test.properties", properties = { "amqp.event.start=true",
		"amqp.event.retry.start=false", "amqp.event.retry.retry-after=1" })
public class AmqpFullCycleTest extends BaseServerTest {

	private static final Logger log = LoggerFactory.getLogger(AmqpFullCycleTest.class);

	private static QpidBrokerTest broker;

	@Inject
	private AmqpTemplate ampqTemplate;

	@Inject
	private EventRepository eventRepo;

	@Inject
	private EventEnricherService enricher;

	@Inject
	private AmqpInboundChannelAdapter amqpMainInbound;

	@BeforeClass
	public static void startup() throws Exception {
		broker = new QpidBrokerTest();
		broker.startBroker();

	}

	@AfterClass
	public static void tearDown() throws Exception {
		broker.stopBroker();
	}

	/**
	 * Tests the full life-cycle:
	 * <ol>
	 * <li>Turn off retry amqp inbound channel by default</li>
	 * <li>turn on ldap exception triggers rollback</li>
	 * <li>Construct Event message for saving</li>
	 * <li>turn on main import</li>
	 * <li>send to import event</li>
	 * </ol>
	 * 
	 * @throws Exception
	 */
	@Test
	public void fullCycle() throws Exception {

		final LdapEventAttributeEnricher ldapEnricher = new LdapEventAttributeEnricher();

		final LdapContextSource contextSource = new LdapContextSource();
		contextSource.setUrl("ldap://null/");
		contextSource.setBase("o=null");
		contextSource.setUserDn("nobody");
		contextSource.setPassword("null");
		contextSource.afterPropertiesSet();

		ldapEnricher.setLdap(new LdapTemplate(contextSource));
		ldapEnricher.setPrincipalFieldName("principalName");
		ldapEnricher.setSourcePrincipalLookupQuery("(&(ObjectClass=CardiffAccount)(cn=?ppn))");
		ldapEnricher.setPrincipalSchoolSourceAttribute("CardiffIDManDept");
		ldapEnricher.setPrincipalAffiliationSourceAttribute("CardiffIDManAffiliation");

		enricher.setEnricher(ldapEnricher);
		enricher.setExceptionTriggersRollbqck(true);

		final Event mockEvent = mockShibEvent("scmps2");

		final ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new com.fasterxml.jackson.datatype.joda.JodaModule());
		final String json = mapper.writeValueAsString(mockEvent);
		log.debug("Constructed JSON {}", json);

		final org.springframework.amqp.core.MessageProperties props = new org.springframework.amqp.core.MessageProperties();
		props.setContentType("application/json");
		props.setHeader("__TypeId__", "uk.ac.cardiff.model.event.ShibbolethIdpAuthenticationEvent");

		final Message forQueue = new Message(json.getBytes(), props);

		log.info("Sending to main harvest queue, expecting retry needed [{}]", forQueue);
		ampqTemplate.send("raptor.harvest.test", forQueue);

		final Message recMsg = ampqTemplate.receive("raptor.harvest.test-retry", 2000);
		Assert.assertNotNull(recMsg);
		log.debug("Retrieved from retry queue [{}]", recMsg);

		// turn off ldap exception issue
		enricher.setExceptionTriggersRollbqck(false);

		ampqTemplate.send("raptor.harvest.test", forQueue);

		final long numberInDb = eventRepo.count();
		log.info("Has {} events stored in the database", numberInDb);
		Assert.assertEquals(1, numberInDb);

		final Event inRepo = eventRepo.findOne(mockEvent.getEventId());

		log.info("Has found repository event [{}]", inRepo);

		Assert.assertNotNull(inRepo);
		Assert.assertEquals(mockEvent.getEventId(), inRepo.getEventId());

	}

}

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

import uk.ac.cardiff.model.event.Event;
import uk.ac.cardiff.raptor.server.amqp.QpidBrokerTest;
import uk.ac.cardiff.raptor.server.enrich.EventEnricherService;
import uk.ac.cardiff.raptor.server.enrich.LdapEventAttributeEnricher;

@TestPropertySource(locations = "/application-test.properties", properties = { "amqp.event.start=true",
		"amqp.event.retry.start=false" })
public class AmqpLdapInboundTest extends BaseServerTest {

	private static final Logger log = LoggerFactory.getLogger(AmqpLdapInboundTest.class);

	private static QpidBrokerTest broker;

	@Inject
	private EventEnricherService enricher;

	@Inject
	private AmqpInboundChannelAdapter amqpInbound;

	@Inject
	private AmqpTemplate ampqTemplate;

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
	public void testLdapUnavailableRollback() throws Exception {
		Assert.assertNotNull(ampqTemplate);
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

		ampqTemplate.convertAndSend("raptor.harvest.test", mockEvent);

		final Message recMsg = ampqTemplate.receive("raptor.harvest.test-retry", 5000);

		Assert.assertNotNull(recMsg);

		compareEvent(recMsg, mockEvent, 1);

	}

}

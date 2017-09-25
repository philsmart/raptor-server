package uk.ac.cardiff.raptor.server;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.context.support.GenericWebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.cardiff.model.event.Event;
import uk.ac.cardiff.model.event.EzproxyAuthenticationEvent;
import uk.ac.cardiff.model.event.ShibbolethIdpAuthenticationEvent;
import uk.ac.cardiff.model.event.auxiliary.EventMetadata;
import uk.ac.cardiff.model.event.auxiliary.PrincipalInformation;
import uk.ac.cardiff.raptor.server.error.ProcessingErrorConstants;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = RaptorServerApplication.class)
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class BaseServerTest {

	private static final Logger log = LoggerFactory.getLogger(BaseServerTest.class);

	@Inject
	GenericWebApplicationContext context;

	protected Event mockShibEvent(final String user) {

		final ShibbolethIdpAuthenticationEvent event = new ShibbolethIdpAuthenticationEvent();

		event.setPrincipalName(user);
		event.setEventTime(new DateTime());

		event.setAttributes(new String[] { "attr1,attr2,attr2" });
		event.setResourceId("https://myfakeservice.com/");
		event.setResourceHost("localhost");
		final PrincipalInformation pinfo = new PrincipalInformation();
		// pinfo.setAffiliation("Staff");
		// pinfo.setSchool("COMSC");
		event.setPrincipalInformation(pinfo);
		final EventMetadata meta = new EventMetadata();
		meta.setEntityId("http://localhost.test");
		meta.setOrganisationName("CU Test");
		meta.setServiceName("local test service");

		final int eventHash = event.getHashCode();
		log.debug("Event for {} has hash {}", event.getPrincipalName(), eventHash);
		event.setEventId(event.getHashCode());

		return event;
	}

	protected Event mockEzproxyEvent(final String user) {

		final EzproxyAuthenticationEvent event = new EzproxyAuthenticationEvent();

		event.setPrincipalName(user);
		event.setEventTime(new DateTime());

		event.setRequesterIp("192.1678.0.1");
		event.setResourceId("https://myfakeservice.com/");
		event.setResourceHost("localhost");
		final PrincipalInformation pinfo = new PrincipalInformation();
		// pinfo.setAffiliation("Staff");
		// pinfo.setSchool("COMSC");
		event.setPrincipalInformation(pinfo);
		final EventMetadata meta = new EventMetadata();
		meta.setEntityId("http://localhost.test");
		meta.setOrganisationName("CU Test");
		meta.setServiceName("local test service");

		final int eventHash = event.getHashCode();
		log.debug("Event for {} has hash {}", event.getPrincipalName(), eventHash);
		event.setEventId(event.getHashCode());

		return event;

	}

	protected Event compareEvent(final Message recMsg, final Event mockEvent, final int expectedRetryCount) {
		final Event event = convertShibbolethIdpAuthenticationEvent(recMsg);

		final Map<String, Object> headers = recMsg.getMessageProperties().getHeaders();

		// needs EventProcessExceptionService.ERROR_HEADER in the rejected
		// message
		final Object errorHeader = headers.get(ProcessingErrorConstants.ERROR_HEADER);
		final Object retryCounter = headers.get(ProcessingErrorConstants.RETRY_COUNT);
		final Object retryTimestamp = headers.get(ProcessingErrorConstants.RETRY_TIMESTAMP);

		Assert.assertNotNull("Message from retry queue must contain an error header", errorHeader);
		Assert.assertNotNull("Message from retry queue must contain a retry count header", retryCounter);
		Assert.assertNotNull("Message from retry queue must contain a retry timestamp header", retryTimestamp);

		log.info("Has Error Header [{}]", errorHeader);
		log.info("Has Retry Counter [{}]", retryCounter);
		log.info("Has Retry Timestamp [{}]", retryTimestamp);

		if (retryCounter instanceof Integer) {
			final int retryCounterInt = (Integer) retryCounter;
			Assert.assertTrue("Retry counter expected " + expectedRetryCount + " is " + retryCounterInt,
					retryCounterInt == expectedRetryCount);

		} else {
			Assert.fail("Retry Count Header was not an int");
		}

		Assert.assertNotNull("Could not convert message to event", event);

		log.info("Has recieved retry event [{}]", event);

		Assert.assertEquals("Event ID of retry event is not the same as the input Event", event.getEventId(),
				mockEvent.getEventId());

		log.info("Event ID [{}] of input was the same as the Event ID [{}] of the retry event. All good.",
				event.getEventId(), event.getEventId());

		return event;
	}

	protected Event convertShibbolethIdpAuthenticationEvent(final Message fromQueue) {
		final byte[] bytes = fromQueue.getBody();

		if (bytes != null) {
			final ObjectMapper mapper = new ObjectMapper();
			try {
				final ShibbolethIdpAuthenticationEvent event = mapper.readValue(bytes,
						ShibbolethIdpAuthenticationEvent.class);
				return event;
			} catch (final IOException e) {
				log.error("Could not convert message to Event", e);
				return null;
			}
		}
		return null;

	}

	protected Event mockShibEventLongResourceId(final String user) {

		final ShibbolethIdpAuthenticationEvent event = new ShibbolethIdpAuthenticationEvent();

		event.setPrincipalName(user);
		event.setEventTime(new DateTime());

		event.setAttributes(new String[] { "attr1,attr2,attr2" });
		event.setResourceId("https://myfakeservice.com/xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
				+ "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
				+ "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
				+ "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
				+ "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
				+ "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
				+ "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
				+ "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
		event.setResourceHost("localhost");
		final PrincipalInformation pinfo = new PrincipalInformation();
		// pinfo.setAffiliation("Staff");
		// pinfo.setSchool("COMSC");
		event.setPrincipalInformation(pinfo);
		final EventMetadata meta = new EventMetadata();
		meta.setEntityId("http://localhost.test");
		meta.setOrganisationName("CU Test");
		meta.setServiceName("local test service");

		final int eventHash = event.getHashCode();
		log.debug("Event for {} has hash {}", event.getPrincipalName(), eventHash);
		event.setEventId(event.getHashCode());

		return event;
	}

	protected Event mockEventFixedId(final String user) {

		final ShibbolethIdpAuthenticationEvent event = new ShibbolethIdpAuthenticationEvent();

		event.setEventId(980348989);
		event.setPrincipalName(user);
		event.setEventTime(new DateTime());

		event.setAttributes(new String[] { "attr1,attr2,attr2" });
		event.setResourceId("https://myfakeservice.com/");
		event.setResourceHost("localhost");
		final PrincipalInformation pinfo = new PrincipalInformation();
		// pinfo.setAffiliation("Staff");
		// pinfo.setSchool("COMSC");
		event.setPrincipalInformation(pinfo);
		final EventMetadata meta = new EventMetadata();
		meta.setEntityId("http://localhost.test");
		meta.setOrganisationName("CU Test");
		meta.setServiceName("local test service");

		return event;
	}

}

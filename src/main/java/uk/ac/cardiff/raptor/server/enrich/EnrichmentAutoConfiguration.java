package uk.ac.cardiff.raptor.server.enrich;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;

/**
 * Auto configure the source and {@link EventAttributeEnricher} type based on
 * the attribute.enrich.source property. Either LDAP or DB.
 * 
 * @author philsmart
 *
 */
@Configuration
public class EnrichmentAutoConfiguration {

	private static final Logger log = LoggerFactory.getLogger(EnrichmentAutoConfiguration.class);

	@Inject
	Environment env;

	private LdapContextSource contextSource() {

		final LdapContextSource contextSource = new LdapContextSource();
		contextSource.setUrl(env.getRequiredProperty("attribute.enrich.ldap.url"));
		contextSource.setBase(env.getRequiredProperty("attribute.enrich.ldap.base"));
		contextSource.setUserDn(env.getRequiredProperty("attribute.enrich.ldap.user"));
		contextSource.setPassword(env.getRequiredProperty("attribute.enrich.ldap.password"));
		contextSource.afterPropertiesSet();
		log.info("Constructing LDAP Source, url [{}], user [{}]", env.getRequiredProperty("attribute.enrich.ldap.url"),
				env.getRequiredProperty("attribute.enrich.ldap.user"));
		return contextSource;
	}

	private LdapTemplate ldapTemplate() {
		return new LdapTemplate(contextSource());
	}

	@ConditionalOnProperty(prefix = "attribute.enrich", name = "source", havingValue = "ldap")
	@Bean("eventEnricher")
	public EventAttributeEnricher ldapEnricher() {
		log.info("Creating an LDAP Event Attribute Enricher");
		final LdapEventAttributeEnricher enricher = new LdapEventAttributeEnricher();
		enricher.setLdap(ldapTemplate());
		enricher.setPrincipalFieldName(env.getRequiredProperty("attribute.enrich.ldap.principalFieldName"));
		enricher.setSourcePrincipalLookupQuery(
				env.getRequiredProperty("attribute.enrich.ldap.sourcePrincipalLookupQuery"));
		enricher.setPrincipalSchoolSourceAttribute(
				env.getProperty("attribute.enrich.ldap.principalSchoolSourceAttribute"));
		enricher.setPrincipalAffiliationSourceAttribute(
				env.getProperty("attribute.enrich.ldap.principalAffiliationSourceAttribute"));
		return enricher;
	}

	@ConditionalOnProperty(prefix = "attribute.enrich", name = "source", havingValue = "none")
	@Bean("eventEnricher")
	public EventAttributeEnricher noOpEnricher() {
		log.info("Creating a No Operation Attribute Enricher");
		final NoOperationEventAttributeEnricher enricher = new NoOperationEventAttributeEnricher();

		return enricher;
	}

	/**
	 * Create the {@link EventEnricherService} using the configured
	 * {@link EventEnricherService}
	 * 
	 * @param eventEnricher
	 *            a {@link EventAttributeEnricher} that is autowired in spring
	 *            by name then type - so make sure only one bean constructed of
	 *            that type.
	 * @return
	 */
	@Bean("eventEnricherService")
	public EventEnricherService enricherService(final EventAttributeEnricher eventEnricher) {
		log.info("Setting up eventEnricherService with EventAttributeEnricher {}",
				eventEnricher.getClass().getSimpleName());
		final EventEnricherService enricher = new EventEnricherService(eventEnricher);
		enricher.setExceptionTriggersRollbqck(
				env.getProperty("attribute.enrich.rollback-on-excepton", Boolean.class, false));
		return enricher;

	}

}

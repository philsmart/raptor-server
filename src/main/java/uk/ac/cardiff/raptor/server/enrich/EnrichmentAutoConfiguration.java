package uk.ac.cardiff.raptor.server.enrich;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.stereotype.Component;

import uk.ac.cardiff.model.event.Event;
import uk.ac.cardiff.raptor.server.enrich.EnrichmentAutoConfiguration.EventAttributeEnricherInformation.ENRICHER_TYPE;

/**
 * Auto configure the source and {@link EventAttributeEnricher} type based on
 * the attribute.enrichers[0] property. Either LDAP or DB.
 * 
 * @author philsmart
 *
 */
@Component
@ConfigurationProperties(prefix = "attribute")
public class EnrichmentAutoConfiguration {

	private static final Logger log = LoggerFactory.getLogger(EnrichmentAutoConfiguration.class);

	private List<EventAttributeEnricherInformation> enrichers;

	@Inject
	Environment env;

	public static class EventAttributeEnricherInformation {

		private Class<? extends Event> forClass;

		public enum ENRICHER_TYPE {
			LDAP, JDBC
		}

		/**
		 * An LDAP or JDBC URL (or other possible).
		 */
		private String url;

		private ENRICHER_TYPE type;

		private String user;

		private String password;

		private String ldapBase;

		private String sourcePrincipalLookupQuery;

		private String principalFieldName;

		private String principalSchoolSourceAttribute;

		private String principalAffiliationSourceAttribute;

		public final String getPassword() {
			return password;
		}

		public final void setPassword(final String password) {
			this.password = password;
		}

		public final String getLdapBase() {
			return ldapBase;
		}

		public final void setLdapBase(final String ldapBase) {
			this.ldapBase = ldapBase;
		}

		public final String getSourcePrincipalLookupQuery() {
			return sourcePrincipalLookupQuery;
		}

		public final void setSourcePrincipalLookupQuery(final String sourcePrincipalLookupQuery) {
			this.sourcePrincipalLookupQuery = sourcePrincipalLookupQuery;
		}

		public final String getPrincipalFieldName() {
			return principalFieldName;
		}

		public final void setPrincipalFieldName(final String principalFieldName) {
			this.principalFieldName = principalFieldName;
		}

		public final String getPrincipalSchoolSourceAttribute() {
			return principalSchoolSourceAttribute;
		}

		public final void setPrincipalSchoolSourceAttribute(final String principalSchoolSourceAttribute) {
			this.principalSchoolSourceAttribute = principalSchoolSourceAttribute;
		}

		public final String getPrincipalAffiliationSourceAttribute() {
			return principalAffiliationSourceAttribute;
		}

		public final void setPrincipalAffiliationSourceAttribute(final String principalAffiliationSourceAttribute) {
			this.principalAffiliationSourceAttribute = principalAffiliationSourceAttribute;
		}

		/**
		 * @return the user
		 */
		public String getUser() {
			return user;
		}

		/**
		 * @param user
		 *            the user to set
		 */
		public void setUser(final String user) {
			this.user = user;
		}

		/**
		 * @return the forClass
		 */
		public Class<? extends Event> getForClass() {
			return forClass;
		}

		/**
		 * @param forClass
		 *            the forClass to set
		 */
		public void setForClass(final Class<? extends Event> forClass) {
			this.forClass = forClass;
		}

		/**
		 * @return the type
		 */
		public ENRICHER_TYPE getType() {
			return type;
		}

		/**
		 * @param type
		 *            the type to set
		 */
		public void setType(final ENRICHER_TYPE type) {
			this.type = type;
		}

		/**
		 * @return the url
		 */
		public String getUrl() {
			return url;
		}

		/**
		 * @param url
		 *            the url to set
		 */
		public void setUrl(final String url) {
			this.url = url;
		}

		@Override
		public String toString() {
			final StringBuilder builder = new StringBuilder();
			builder.append("EventAttributeEnricherInformation [forClass=");
			builder.append(forClass);
			builder.append(", url=");
			builder.append(url);
			builder.append(", type=");
			builder.append(type);
			builder.append(", user=");
			builder.append(user);
			builder.append(", password=");
			builder.append(password);
			builder.append(", ldapBase=");
			builder.append(ldapBase);
			builder.append(", sourcePrincipalLookupQuery=");
			builder.append(sourcePrincipalLookupQuery);
			builder.append(", principalFieldName=");
			builder.append(principalFieldName);
			builder.append(", principalSchoolSourceAttribute=");
			builder.append(principalSchoolSourceAttribute);
			builder.append(", principalAffiliationSourceAttribute=");
			builder.append(principalAffiliationSourceAttribute);
			builder.append("]");
			return builder.toString();
		}

	}

	@PostConstruct
	public void init() {
		log.debug("Has enrichers {}", enrichers);

	}

	/**
	 * Constructs a {@link List} of {@link EventAttributeEnricher}s by using the
	 * values in the auto-property configured
	 * {@link EventAttributeEnricherInformation} list in the ({@code enrichers})
	 * field.
	 * 
	 * @return a configured list of {@link AbstractEventAttributeEnricher}s
	 */
	private List<AbstractEventAttributeEnricher> constructEnrichers() {

		final List<AbstractEventAttributeEnricher> convertedEnrichers = new ArrayList<AbstractEventAttributeEnricher>();

		for (final EventAttributeEnricherInformation info : enrichers) {
			if (info.getType() == ENRICHER_TYPE.LDAP) {
				log.info("Creating an LDAP Event Attribute Enricher");
				final LdapEventAttributeEnricher ldap = new LdapEventAttributeEnricher();

				final LdapContextSource contextSource = new LdapContextSource();
				contextSource.setUrl(info.getUrl());
				contextSource.setBase(info.getLdapBase());
				contextSource.setUserDn(info.getUser());
				if (info.getPassword() == null) {
					contextSource.setPassword(env.getRequiredProperty("attribute.enricher.ldap.password"));
				} else {
					contextSource.setPassword(info.getPassword());
				}
				contextSource.afterPropertiesSet();
				log.info("Constructing LDAP Source, url [{}], user [{}]", info.getUrl(), info.getUser());
				ldap.setForClass(info.getForClass());
				ldap.setLdap(new LdapTemplate(contextSource));
				ldap.setPrincipalFieldName(info.getPrincipalFieldName());
				ldap.setSourcePrincipalLookupQuery(info.getSourcePrincipalLookupQuery());
				ldap.setPrincipalSchoolSourceAttribute(info.getPrincipalSchoolSourceAttribute());
				ldap.setPrincipalAffiliationSourceAttribute(info.getPrincipalAffiliationSourceAttribute());
				convertedEnrichers.add(ldap);
			}
		}

		return convertedEnrichers;

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
	public EventEnricherService enricherService() {
		log.info("Setting up eventEnricherService");
		final EventEnricherService enricher = new EventEnricherService(constructEnrichers());
		enricher.setExceptionTriggersRollbqck(
				env.getProperty("attribute.enrich.rollback-on-excepton", Boolean.class, false));

		return enricher;

	}

	/**
	 * @return the enrichers
	 */
	public List<EventAttributeEnricherInformation> getEnrichers() {
		return enrichers;
	}

	/**
	 * @param enrichers
	 *            the enrichers to set
	 */
	public void setEnrichers(final List<EventAttributeEnricherInformation> enrichers) {
		this.enrichers = enrichers;
	}

}

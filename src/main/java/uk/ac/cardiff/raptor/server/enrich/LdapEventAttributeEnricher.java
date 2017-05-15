package uk.ac.cardiff.raptor.server.enrich;

import static org.springframework.ldap.query.LdapQueryBuilder.query;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;

import uk.ac.cardiff.model.event.AuthenticationEvent;
import uk.ac.cardiff.model.event.Event;
import uk.ac.cardiff.model.event.auxiliary.PrincipalInformation;

/**
 * Ldap attribute enricher, only currently supports adding information from an
 * LDAP source to the {@link PrincipalInformation} field of the
 * {@link AuthenticationEvent} class. This limits possible lookups to the
 * {@link PrincipalInformation#setAffiliation(String)} and
 * {@link PrincipalInformation#setSchool(String)} fields.
 * 
 * @author philsmart
 *
 */
public class LdapEventAttributeEnricher extends AbstractEventAttributeEnricher {

	private static final Logger log = LoggerFactory.getLogger(LdapEventAttributeEnricher.class);

	private LdapTemplate ldap;

	@PostConstruct
	public void init() {
		Objects.requireNonNull(sourcePrincipalLookupQuery);
		Objects.requireNonNull(principalFieldName);
		log.info(
				"Using principal school attribute name [{}], principal affiliation attribute name [{}], principal field name [{}]",
				principalSchoolSourceAttribute, principalAffiliationSourceAttribute, principalFieldName);
	}

	@Override
	public void enrich(@Nonnull final Event event) throws EventAttributeEnricherException {
		Objects.requireNonNull(event);
		try {

			final Optional<Object> value = getPrincipalValueOffEvent(event);
			log.debug("Has Principal Value [{}]", value);

			if (value.isPresent()) {

				final String boundFilter = sourcePrincipalLookupQuery.replace("?ppn", value.get().toString());
				log.debug("Filter is [{}]", boundFilter);

				final List<PrincipalInformation> principalInfos = ldap.search(query().filter(boundFilter),
						(AttributesMapper<PrincipalInformation>) attrs -> {

							final PrincipalInformation information = new PrincipalInformation();

							if (principalAffiliationSourceAttribute != null) {
								information.setAffiliation(
										safeGetForcedString(attrs.get(principalAffiliationSourceAttribute)));
							}
							if (principalSchoolSourceAttribute != null) {
								information.setSchool(safeGetForcedString(attrs.get(principalSchoolSourceAttribute)));
							}

							return information;

						});

				if (principalInfos == null) {
					log.debug("No results from LDAP for principal [{}]", value.get());
					return;
				}
				if (principalInfos.size() == 1) {

				} else {
					log.debug("LDAP has {} results for principal [{}], requires 1 result to attach to event",
							value.get());
				}

				setValueOnObject(event, principalInfos.get(0), "principalInformation");

			}

		} catch (final Throwable e) {
			throw new EventAttributeEnricherException(e);
		}

	}

	private String safeGetForcedString(final Attribute attr) throws NamingException {
		if (attr == null || attr.get() == null) {
			return null;
		}
		return attr.get().toString();
	}

	public LdapTemplate getLdap() {
		return ldap;
	}

	public void setLdap(final LdapTemplate ldap) {
		this.ldap = ldap;
	}

}

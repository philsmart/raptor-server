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

	private String user;

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

				final List<PrincipalInformation> principalInfos = resolvePrincipalInformation(value.get().toString());

				if (principalInfos == null) {
					log.debug("No results from LDAP for principal [{}]", value.get());
					return;
				}
				if (principalInfos.size() == 1) {
					log.debug("LDAP has 1 result for principal {}, attaching principal information", value.get());
					setValueOnObject(event, principalInfos.get(0), "principalInformation");
				} else {
					log.debug("LDAP has {} results for principal [{}], requires 1 result to attach to event",
							principalInfos.size(), value.get());
				}

			}

		} catch (final Throwable e) {
			throw new EventAttributeEnricherException(e);
		}

	}

	/**
	 * Resolve school and affiliation from the principalName by replacing the
	 * ?ppn variable in the {@code sourcePrincipalLookupQuery} and running the
	 * filter against the configured ldap server.
	 * 
	 * @param principalName
	 *            the ppn to resolve school and affiliation for.
	 * @return a {@link List} of {@link PrincipalInformation}.
	 */
	private List<PrincipalInformation> resolvePrincipalInformation(final String principalName) {

		final String boundFilter = sourcePrincipalLookupQuery.replace("?ppn", principalName);
		log.debug("Filter is [{}]", boundFilter);

		final List<PrincipalInformation> principalInfos = ldap.search(query().filter(boundFilter),
				(AttributesMapper<PrincipalInformation>) attrs -> {

					final PrincipalInformation information = new PrincipalInformation();

					if (principalAffiliationSourceAttribute != null) {
						information.setAffiliation(safeGetForcedString(attrs.get(principalAffiliationSourceAttribute)));
					}
					if (principalSchoolSourceAttribute != null) {
						information.setSchool(safeGetForcedString(attrs.get(principalSchoolSourceAttribute)));
					}

					return information;

				});

		return principalInfos;

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

}

package uk.ac.cardiff.raptor.server.enrich;

import static org.springframework.ldap.query.LdapQueryBuilder.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

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
@ThreadSafe
public class LdapEventAttributeEnricher extends AbstractEventAttributeEnricher {

	private static final Logger log = LoggerFactory.getLogger(LdapEventAttributeEnricher.class);

	private LdapTemplate ldap;

	private String user;

	/**
	 * Sets the default cache after write expiry time to 10 minutes if
	 * {@link #getCacheExpiryAfterWriteMs()} is null.
	 */
	private final long DEFAULT_CACHE_EXPIRE_AFTER_WRITE = 600000;

	private final int MAX_CACHE_SIZE = 1000;

	/**
	 * LDAP results cache, can be null if not enabled. The key is principal name,
	 * the value is a {@link PrincipalInformation} object.
	 */
	private Cache<String, PrincipalInformation> cache;

	public void init() {
		Objects.requireNonNull(sourcePrincipalLookupQuery);
		Objects.requireNonNull(principalFieldName);

		log.info(
				"Using principal school attribute name [{}], principal affiliation attribute name [{}], principal field name [{}]",
				principalSchoolSourceAttribute, principalAffiliationSourceAttribute, principalFieldName);

		if (isUseCache()) {

			final long expireAfterWrite = getCacheExpiryAfterWriteMs() == 0 ? DEFAULT_CACHE_EXPIRE_AFTER_WRITE
					: getCacheExpiryAfterWriteMs();

			log.info(
					"LdapEventEnricher has been configured to use a cache of size [{}] and an expire-after-write of [{}ms]",
					MAX_CACHE_SIZE, expireAfterWrite);
			cache = Caffeine.newBuilder().expireAfterWrite(expireAfterWrite, TimeUnit.MILLISECONDS)
					.maximumSize(MAX_CACHE_SIZE).build();
		}
	}

	@Override
	public void enrich(@Nonnull final Event event) throws EventAttributeEnricherException {
		Objects.requireNonNull(event);
		try {

			final Optional<Object> value = getPrincipalValueOffEvent(event);
			log.trace("Event [{}] has principal value [{}]", event.getEventId(), value);

			if (value.isPresent()) {

				final List<PrincipalInformation> principalInfos = resolvePrincipalInformation(value.get().toString());

				if (principalInfos == null) {
					log.trace("No results from LDAP for principal [{}]", value.get());
					return;
				}
				if (principalInfos.size() == 1) {
					log.debug(
							"Event [{}] has 1 result for principal from LDAP [{}], attaching principal information [{},{}]",
							event.getEventId(), value.get(), principalInfos.get(0).getAffiliation(),
							principalInfos.get(0).getSchool());
					setValueOnObject(event, principalInfos.get(0), "principalInformation");

					if (isUseCache()) {
						cache.put(value.get().toString(), principalInfos.get(0));
					}
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
	 * Resolve school and affiliation from the principalName by replacing the ?ppn
	 * variable in the {@code sourcePrincipalLookupQuery} and running the filter
	 * against the configured ldap server. If caching is enabled, the
	 * {@link PrincipalInformation} is first looked up from the cache, only
	 * performing the LDAP search if it can not be found (does not exist, or cache
	 * entry has expired).
	 * 
	 * @param principalName
	 *            the ppn to resolve school and affiliation for.
	 * @return a {@link List} of {@link PrincipalInformation}.
	 */
	private List<PrincipalInformation> resolvePrincipalInformation(final String principalName) {

		if (isUseCache()) {
			log.debug("Performing cache lookup for principal [{}]", principalName);
			final PrincipalInformation found = cache.getIfPresent(principalName);
			log.debug("Principal [{}] was in cache [{}], PrincipalInformation is [{}]", principalName,
					found == null ? "no" : "yes", found);
			if (found != null) {
				final List<PrincipalInformation> information = new ArrayList<PrincipalInformation>();
				information.add(found);
				return information;
				// else go find from LDAP
			}
		}

		final String boundFilter = sourcePrincipalLookupQuery.replace("?ppn", principalName);
		log.trace("LDAP Filter is [{}]", boundFilter);

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

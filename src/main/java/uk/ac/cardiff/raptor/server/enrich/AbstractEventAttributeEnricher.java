package uk.ac.cardiff.raptor.server.enrich;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Caffeine;

import uk.ac.cardiff.model.event.Event;
import uk.ac.cardiff.model.event.auxiliary.PrincipalInformation;

/**
 * Base all {@link EventAttributeEnricher}s should extend. Contains
 * configuration of the attribute lookups used for enrichement. These only
 * include support for {@link PrincipalInformation#setAffiliation(String)} and
 * {@link PrincipalInformation#setSchool(String)};
 * 
 * @author philsmart
 *
 */
public abstract class AbstractEventAttributeEnricher implements EventAttributeEnricher {

	private static final Logger log = LoggerFactory.getLogger(AbstractEventAttributeEnricher.class);

	private Class<? extends Event> forClass;

	/**
	 * The name of the attribute in the source that represents the principals school
	 * name;
	 */
	protected String principalSchoolSourceAttribute;

	/**
	 * The name of the attribute in the source that represents the principals
	 * affiliation with the organisation.
	 */
	protected String principalAffiliationSourceAttribute;

	/**
	 * The principal lookup query to execute over the source e.g. LDAP Filter.
	 */
	protected String sourcePrincipalLookupQuery;

	/**
	 * The name of the field in the {@link Event} model that is used as the
	 * principal name with which to resolve attributes from the source.
	 */
	protected String principalFieldName;

	/**
	 * If set to true, a cache (if present) is initalised and used. Actual cache and
	 * its implementation is left to the implementing class. Implementing class does
	 * not need to guarantee the use of a cache.
	 */
	private boolean useCache;

	/**
	 * Specifies that each entry should be automatically removed from the cache once
	 * this fixed duration has elapsed after the entry's creation, or the most
	 * recent replacement of its value. Semantics should be comparable to
	 * {@link Caffeine#expireAfterWrite(long, java.util.concurrent.TimeUnit)}. Does
	 * not need to be set if cache is not used.
	 */
	private long cacheExpiryAfterWriteMs;

	/**
	 * Gets the value of the {@code principalFieldName} off the {@link Event}
	 * object.
	 * 
	 * @param event
	 *            the {@link Event} to retrieve the {@code principalFieldName} from.
	 * @return an {@link Optional} of the value.
	 */
	protected Optional<Object> getPrincipalValueOffEvent(final Event event) {
		try {
			final PropertyDescriptor princiapalProperty = new PropertyDescriptor(principalFieldName, event.getClass());
			final Method readMethod = princiapalProperty.getReadMethod();
			log.trace("Has method {}", readMethod);
			if (readMethod != null) {
				final Object result = readMethod.invoke(event, new Object[] {});
				return Optional.ofNullable(result);
			}
		} catch (final IntrospectionException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			// do nothing
		}
		return Optional.empty();
	}

	protected void setValueOnObject(final Event event, final Object value, final String field) {
		try {
			final PropertyDescriptor princiapalProperty = new PropertyDescriptor(field, event.getClass());
			final Method writeMethod = princiapalProperty.getWriteMethod();
			log.trace("Has method {}", writeMethod);
			if (writeMethod != null) {
				final Object result = writeMethod.invoke(event, new Object[] { value });

			}
		} catch (final IntrospectionException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			// do nothing

		}

	}

	public String getPrincipalFieldName() {
		return principalFieldName;
	}

	public void setPrincipalFieldName(final String principalFieldName) {
		this.principalFieldName = principalFieldName;
	}

	public String getSourcePrincipalLookupQuery() {
		return sourcePrincipalLookupQuery;
	}

	public void setSourcePrincipalLookupQuery(final String sourcePrincipalLookupQuery) {
		this.sourcePrincipalLookupQuery = sourcePrincipalLookupQuery;
	}

	public String getPrincipalSchoolSourceAttribute() {
		return principalSchoolSourceAttribute;
	}

	public void setPrincipalSchoolSourceAttribute(final String principalSchoolSourceAttribute) {
		this.principalSchoolSourceAttribute = principalSchoolSourceAttribute;
	}

	/**
	 * @return the principalAffiliationSourceAttribute
	 */
	public String getPrincipalAffiliationSourceAttribute() {
		return principalAffiliationSourceAttribute;
	}

	/**
	 * @param principalAffiliationSourceAttribute
	 *            the principalAffiliationSourceAttribute to set
	 */
	public void setPrincipalAffiliationSourceAttribute(final String principalAffiliationSourceAttribute) {
		this.principalAffiliationSourceAttribute = principalAffiliationSourceAttribute;
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
	 * @return the useCache
	 */
	public boolean isUseCache() {
		return useCache;
	}

	/**
	 * @param useCache
	 *            the useCache to set
	 */
	public void setUseCache(final boolean useCache) {
		this.useCache = useCache;
	}

	/**
	 * @return the cacheExpiryAfterWriteMs
	 */
	public long getCacheExpiryAfterWriteMs() {
		return cacheExpiryAfterWriteMs;
	}

	/**
	 * @param cacheExpiryAfterWriteMs
	 *            the cacheExpiryAfterWriteMs to set
	 */
	public void setCacheExpiryAfterWriteMs(final long cacheExpiryAfterWriteMs) {
		this.cacheExpiryAfterWriteMs = cacheExpiryAfterWriteMs;
	}

}

package uk.ac.cardiff.raptor.server.dao;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventStore {

	private static final Logger log = LoggerFactory.getLogger(EventStore.class);

	@Inject
	private JdbcTemplate jdbc;

	@Autowired
	EntityManager entityManager;

	@PostConstruct
	public void setup() {
		log.info("Has injected JDBC template [{}]", jdbc);

		// final int count = jdbc.queryForObject("SELECT count(*) FROM
		// ShibIdpAuthE", Integer.class);

		// log.debug("Shibboleth table has {} entries", count);

		// System.exit(1);

	}

	public JdbcTemplate getJdbc() {
		return jdbc;
	}

	public void setJdbc(final JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

}

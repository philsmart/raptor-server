package uk.ac.cardiff.raptor.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(locations = "/application-test.properties", properties = { "amqp.event.start=false",
		"amqp.event.retry.start=false" })
public class DbSchemaTest extends BaseServerTest {

	private static final Logger log = LoggerFactory.getLogger(DbSchemaTest.class);

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	public void testShibolethSchema() throws SQLException {
		log.debug("JDBC template is [{}]", jdbc);
		final DatabaseMetaData meta = jdbc.getDataSource().getConnection().getMetaData();

		final String[] columnsThatShouldExist = new String[] { "EVENT_ID", "ORGANISATION_NAME", "RAPTOR_ENTITY_ID",
				"SERVICE_NAME", "EVENT_TIME", "EVENT_TYPE", "RESOURCE_HOST", "RESOURCE_ID", "RESOURCE_ID_CATEGORY",
				"SERVICE_HOST", "SERVICE_ID", "AUTHENTICATION_TYPE", "AFFILIATION", "SCHOOL", "PRINCIPAL_NAME",
				"ATTRIBUTES", "REQUEST_BINDING", "RESPONSE_BINDING" };

		final List<String> columnsThatShouldExistList = new ArrayList<String>(Arrays.asList(columnsThatShouldExist));

		final ResultSet resultSet = meta.getColumns(null, null, "SHIB_IDP_AUTH_EVENT", null);
		int noColumns = 0;
		while (resultSet.next()) {
			final String name = resultSet.getString("COLUMN_NAME");
			final String type = resultSet.getString("TYPE_NAME");
			final int size = resultSet.getInt("COLUMN_SIZE");

			log.debug("Column name: [" + name + "]; type: [" + type + "]; size: [" + size + "]");
			assertThat(columnsThatShouldExistList).contains(name);
			noColumns++;

		}
		assertThat(noColumns).isEqualTo(columnsThatShouldExist.length);

	}
}

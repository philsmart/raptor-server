package uk.ac.cardiff.raptor.server.config;

import java.io.IOException;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.hsqldb.persist.HsqlProperties;
import org.hsqldb.server.Server;
import org.hsqldb.server.ServerAcl.AclFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;

/**
 * Used to configure a HSQLDB Server if hsqldb is the selected provider
 * 
 * @author philsmart
 *
 */
@Configuration
public class DataSourceConfiguration {

	private static final Logger log = LoggerFactory.getLogger(DataSourceConfiguration.class);

	@Inject
	private Environment env;

	/**
	 * A method for configuring a HSQLDB datasource. Spring would auto-configure
	 * this if this bean were omitted. However, we construct it here so as to
	 * make it dependOn the {@link #hsqldbServer()} bean. That is, the HSQLDB
	 * server is constructed before the DataSource is initialised. This bean is
	 * only constructed if the spring.datasource.driver-class-name has a value
	 * of org.hsqldb.jdbc.JDBCDriver.
	 * 
	 * @return a HSQLDB {@link DataSource}.
	 */
	@ConditionalOnProperty(prefix = "spring.datasource", name = "driver-class-name", havingValue = "org.hsqldb.jdbc.JDBCDriver")
	@ConfigurationProperties(prefix = "spring.datasource")
	@Bean("dataSource")
	@DependsOn("hsqldbServer")
	public DataSource hsqldbServerDataSource() {

		return DataSourceBuilder.create().build();
	}

	/**
	 * This bean configures a HSQLDB {@link Server} instance. This improves upon
	 * the normal in-process HSQLDB server as it allows shared access.
	 * 
	 * @return a {@link Server}.
	 * @throws IOException
	 * @throws AclFormatException
	 */
	@ConditionalOnProperty(prefix = "spring.datasource", name = "driver-class-name", havingValue = "org.hsqldb.jdbc.JDBCDriver")
	@Bean(name = "hsqldbServer", initMethod = "start", destroyMethod = "stop")
	public Server hsqldbServer() throws IOException, AclFormatException {

		log.info(
				"Configuring HSQLDB Server programatically. PLEASE NOTE, THIS SHOULD NOT HAPPEN IF ANOTHER DB PROVIDER HAS BEEN SPECIFIED. This auto-configured"
						+ " when the spring.datasource.driver-class-name having a value of org.hsqldb.jdbc.JDBCDriver");

		final HsqlProperties props = new HsqlProperties();

		final String hsqldbServerUsername = env.getProperty("raptor.hsqldb.server.username", "raptor-db-user");
		final String hsqldbServerPassword = env.getProperty("raptor.hsqldb.server.password", "raptor-db-pass");

		final Server server = new Server();
		props.setProperty("server.database.0", "file:server-db/muadb;hsqldb.default_table_type=cached;user="
				+ hsqldbServerUsername + ";password=" + hsqldbServerPassword);
		props.setProperty("server.dbname.0", "muadb");
		props.setProperty("hsqldb.default_table_type", "cached");

		server.setTrace(true);
		server.setProperties(props);
		server.setNoSystemExit(true);

		return server;
	}

}

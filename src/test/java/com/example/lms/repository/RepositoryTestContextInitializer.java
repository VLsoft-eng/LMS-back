package com.example.lms.repository;

import org.flywaydb.core.Flyway;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

/**
 * Starts PostgreSQL container and runs Flyway before Spring context, so schema exists when Hibernate validates.
 */
public class RepositoryTestContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

	private static final PostgreSQLContainer<?> POSTGRES =
			new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

	static {
		POSTGRES.start();
	}

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		// Run Flyway so tables exist before Hibernate validates
		Flyway.configure()
				.dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
				.locations("classpath:db/migration")
				.load()
				.migrate();

		applicationContext.getEnvironment().getPropertySources().addFirst(
				new MapPropertySource("testcontainers", Map.of(
						"spring.datasource.url", POSTGRES.getJdbcUrl(),
						"spring.datasource.username", POSTGRES.getUsername(),
						"spring.datasource.password", POSTGRES.getPassword()
				)));
	}
}

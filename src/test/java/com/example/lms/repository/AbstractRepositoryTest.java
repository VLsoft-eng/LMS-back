package com.example.lms.repository;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.EnabledIf;

/**
 * Base class for repository tests. Uses RepositoryTestContextInitializer to start PostgreSQL
 * and run Flyway before context, so schema exists when Hibernate validates.
 * Tests are skipped when Docker is not available.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@ContextConfiguration(initializers = RepositoryTestContextInitializer.class)
@EnabledIf(
		expression = "#{T(com.example.lms.DockerAvailability).isAvailable()}",
		loadContext = false,
		reason = "Docker is not available — start Docker Desktop to run repository tests"
)
abstract class AbstractRepositoryTest {
}

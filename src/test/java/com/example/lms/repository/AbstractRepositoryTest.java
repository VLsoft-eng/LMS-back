package com.example.lms.repository;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

/**
 * Base class for repository tests. Uses RepositoryTestContextInitializer to start PostgreSQL
 * and run Flyway before context, so schema exists when Hibernate validates.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@ContextConfiguration(initializers = RepositoryTestContextInitializer.class)
abstract class AbstractRepositoryTest {
}

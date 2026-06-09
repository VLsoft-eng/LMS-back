package com.example.lms.bdd;

import com.example.lms.DockerAvailability;
import com.example.lms.repository.RepositoryTestContextInitializer;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

/**
 * TICKET #9185: Cucumber Spring configuration.
 * Boots the full application context with Testcontainers PostgreSQL.
 * Mirrors AbstractIntegrationTest setup.
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ContextConfiguration(initializers = RepositoryTestContextInitializer.class)
public class CucumberSpringConfiguration {
    // Spring context is shared across all Cucumber scenarios in the same run
}

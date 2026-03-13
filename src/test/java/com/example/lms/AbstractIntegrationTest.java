package com.example.lms;

import com.example.lms.repository.RepositoryTestContextInitializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.EnabledIf;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Base class for all integration tests.
 * Starts a full Spring context against a real PostgreSQL via Testcontainers.
 * Test isolation is handled per subclass (e.g. @BeforeEach cleanup).
 * Tests are skipped when Docker is not available.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ContextConfiguration(initializers = RepositoryTestContextInitializer.class)
@EnabledIf(
		expression = "#{T(com.example.lms.DockerAvailability).isAvailable()}",
		loadContext = false,
		reason = "Docker is not available — start Docker Desktop to run integration tests"
)
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    protected final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
}

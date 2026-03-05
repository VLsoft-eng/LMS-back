package com.example.lms;

import com.example.lms.repository.RepositoryTestContextInitializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Base class for all integration tests.
 * Starts a full Spring context against a real PostgreSQL via Testcontainers.
 * Test isolation is handled per subclass (e.g. @BeforeEach cleanup).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ContextConfiguration(initializers = RepositoryTestContextInitializer.class)
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    protected final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
}

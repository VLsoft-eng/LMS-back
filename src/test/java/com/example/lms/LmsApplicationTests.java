package com.example.lms;

import com.example.lms.repository.RepositoryTestContextInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.EnabledIf;

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = RepositoryTestContextInitializer.class)
@EnabledIf(
		expression = "#{T(com.example.lms.DockerAvailability).isAvailable()}",
		loadContext = false,
		reason = "Docker is not available — start Docker Desktop to run this test"
)
class LmsApplicationTests {

	@Test
	void contextLoads() {
	}

}

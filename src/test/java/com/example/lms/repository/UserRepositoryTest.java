package com.example.lms.repository;

import com.example.lms.entity.UserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TICKET-BE-03: Tests for UserRepository (TDD — tests first).
 */
class UserRepositoryTest extends AbstractRepositoryTest {

	@Autowired
	UserRepository userRepository;

	@Test
	void findByEmail_shouldReturnUser_whenEmailExists() {
		// Given
		String email = "teacher@lms.test";
		UserEntity user = UserEntity.builder()
				.firstName("Иван")
				.lastName("Иванов")
				.email(email)
				.passwordHash("$2a$12$hash")
				.build();
		userRepository.save(user);

		// When
		Optional<UserEntity> found = userRepository.findByEmail(email);

		// Then
		assertThat(found).isPresent();
		assertThat(found.get().getEmail()).isEqualTo(email);
		assertThat(found.get().getFirstName()).isEqualTo("Иван");
	}

	@Test
	void findByEmail_shouldReturnEmpty_whenEmailNotExists() {
		// When
		Optional<UserEntity> found = userRepository.findByEmail("nonexistent@lms.test");

		// Then
		assertThat(found).isEmpty();
	}
}

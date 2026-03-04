package com.example.lms.repository;

import com.example.lms.entity.ClassEntity;
import com.example.lms.entity.ClassMemberEntity;
import com.example.lms.entity.Role;
import com.example.lms.entity.UserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TICKET-BE-03: Tests for ClassMemberRepository (TDD — tests first).
 */
class ClassMemberRepositoryTest extends AbstractRepositoryTest {

	@Autowired
	ClassMemberRepository classMemberRepository;
	@Autowired
	ClassRepository classRepository;
	@Autowired
	UserRepository userRepository;

	@Test
	void findByClassIdAndUserId_shouldReturnMember_whenExists() {
		// Given
		UserEntity user = userRepository.save(UserEntity.builder()
				.firstName("U")
				.lastName("U")
				.email("u@member.test")
				.passwordHash("hash")
				.build());
		ClassEntity cls = classRepository.save(ClassEntity.builder()
				.name("Test Class")
				.code("MEMB001")
				.ownerId(user.getId())
				.build());
		ClassMemberEntity member = classMemberRepository.save(ClassMemberEntity.builder()
				.classId(cls.getId())
				.userId(user.getId())
				.role(Role.OWNER)
				.build());

		// When
		Optional<ClassMemberEntity> found = classMemberRepository.findByClassIdAndUserId(cls.getId(), user.getId());

		// Then
		assertThat(found).isPresent();
		assertThat(found.get().getRole()).isEqualTo(Role.OWNER);
	}

	@Test
	void findByClassIdAndUserId_shouldReturnEmpty_whenNotMember() {
		// Given
		UserEntity user = userRepository.save(UserEntity.builder()
				.firstName("U")
				.lastName("U")
				.email("u2@member.test")
				.passwordHash("hash")
				.build());
		ClassEntity cls = classRepository.save(ClassEntity.builder()
				.name("Other Class")
				.code("MEMB002")
				.ownerId(user.getId())
				.build());

		// When — другой user не в классе
		Optional<ClassMemberEntity> found = classMemberRepository.findByClassIdAndUserId(cls.getId(), java.util.UUID.randomUUID());

		// Then
		assertThat(found).isEmpty();
	}
}

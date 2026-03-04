package com.example.lms.repository;

import com.example.lms.entity.ClassEntity;
import com.example.lms.entity.ClassMemberEntity;
import com.example.lms.entity.Role;
import com.example.lms.entity.UserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TICKET-BE-03: Tests for ClassRepository (TDD — tests first).
 */
class ClassRepositoryTest extends AbstractRepositoryTest {

	@Autowired
	ClassRepository classRepository;
	@Autowired
	UserRepository userRepository;
	@Autowired
	ClassMemberRepository classMemberRepository;

	@Test
	void findAllByMembersUserId_shouldReturnClasses_whenUserIsMember() {
		// Given
		UserEntity owner = userRepository.save(UserEntity.builder()
				.firstName("Owner")
				.lastName("User")
				.email("owner@class.test")
				.passwordHash("hash")
				.build());
		UserEntity member = userRepository.save(UserEntity.builder()
				.firstName("Member")
				.lastName("User")
				.email("member@class.test")
				.passwordHash("hash")
				.build());
		ClassEntity c1 = classRepository.save(ClassEntity.builder()
				.name("Class One")
				.code("CODE0001")
				.ownerId(owner.getId())
				.build());
		ClassEntity c2 = classRepository.save(ClassEntity.builder()
				.name("Class Two")
				.code("CODE0002")
				.ownerId(owner.getId())
				.build());
		classMemberRepository.save(ClassMemberEntity.builder()
				.classId(c1.getId())
				.userId(member.getId())
				.role(Role.STUDENT)
				.build());
		classMemberRepository.save(ClassMemberEntity.builder()
				.classId(c2.getId())
				.userId(member.getId())
				.role(Role.TEACHER)
				.build());

		// When
		List<ClassEntity> classes = classRepository.findAllByMembersUserId(member.getId());

		// Then
		assertThat(classes).hasSize(2);
		assertThat(classes).extracting(ClassEntity::getName).containsExactlyInAnyOrder("Class One", "Class Two");
	}

	@Test
	void existsByCode_shouldReturnTrue_whenCodeExists() {
		// Given
		UserEntity owner = userRepository.save(UserEntity.builder()
				.firstName("O")
				.lastName("O")
				.email("o@code.test")
				.passwordHash("hash")
				.build());
		classRepository.save(ClassEntity.builder()
				.name("By Code")
				.code("UNIQ001")
				.ownerId(owner.getId())
				.build());

		// When
		boolean exists = classRepository.existsByCode("UNIQ001");

		// Then
		assertThat(exists).isTrue();
	}

	@Test
	void existsByCode_shouldReturnFalse_whenCodeNotExists() {
		// When
		boolean exists = classRepository.existsByCode("NONEXIST");

		// Then
		assertThat(exists).isFalse();
	}
}

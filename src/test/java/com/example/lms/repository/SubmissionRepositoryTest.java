package com.example.lms.repository;

import com.example.lms.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TICKET-BE-03: Tests for SubmissionRepository (TDD — tests first).
 */
class SubmissionRepositoryTest extends AbstractRepositoryTest {

	@Autowired
	SubmissionRepository submissionRepository;
	@Autowired
	AssignmentRepository assignmentRepository;
	@Autowired
	ClassRepository classRepository;
	@Autowired
	UserRepository userRepository;

	@Test
	void findByAssignmentIdAndStudentId_shouldReturnSubmission_whenExists() {
		// Given
		UserEntity teacher = userRepository.save(UserEntity.builder()
				.firstName("T")
				.lastName("T")
				.email("t@sub.test")
				.passwordHash("hash")
				.build());
		UserEntity student = userRepository.save(UserEntity.builder()
				.firstName("S")
				.lastName("S")
				.email("s@sub.test")
				.passwordHash("hash")
				.build());
		ClassEntity cls = classRepository.save(ClassEntity.builder()
				.name("Sub Class")
				.code("SUB0001")
				.ownerId(teacher.getId())
				.build());
		AssignmentEntity assignment = assignmentRepository.save(AssignmentEntity.builder()
				.classId(cls.getId())
				.title("Task 1")
				.createdBy(teacher.getId())
				.build());
		SubmissionEntity submission = submissionRepository.save(SubmissionEntity.builder()
				.assignmentId(assignment.getId())
				.studentId(student.getId())
				.answerText("My answer")
				.submittedAt(Instant.now())
				.build());

		// When
		Optional<SubmissionEntity> found = submissionRepository.findByAssignmentIdAndStudentId(assignment.getId(), student.getId());

		// Then
		assertThat(found).isPresent();
		assertThat(found.get().getAnswerText()).isEqualTo("My answer");
	}

	@Test
	void findByAssignmentIdAndStudentId_shouldReturnEmpty_whenNoSubmission() {
		// Given
		UserEntity teacher = userRepository.save(UserEntity.builder()
				.firstName("T2")
				.lastName("T2")
				.email("t2@sub.test")
				.passwordHash("hash")
				.build());
		UserEntity student = userRepository.save(UserEntity.builder()
				.firstName("S2")
				.lastName("S2")
				.email("s2@sub.test")
				.passwordHash("hash")
				.build());
		ClassEntity cls = classRepository.save(ClassEntity.builder()
				.name("Sub Class 2")
				.code("SUB0002")
				.ownerId(teacher.getId())
				.build());
		AssignmentEntity assignment = assignmentRepository.save(AssignmentEntity.builder()
				.classId(cls.getId())
				.title("Task 2")
				.createdBy(teacher.getId())
				.build());

		// When — студент ещё не сдал
		Optional<SubmissionEntity> found = submissionRepository.findByAssignmentIdAndStudentId(assignment.getId(), student.getId());

		// Then
		assertThat(found).isEmpty();
	}
}

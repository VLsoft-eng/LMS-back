package com.example.lms.service;

import com.example.lms.dto.GradeRequest;
import com.example.lms.dto.SubmissionDto;
import com.example.lms.entity.*;
import com.example.lms.exception.ForbiddenException;
import com.example.lms.exception.ResourceNotFoundException;
import com.example.lms.repository.AssignmentRepository;
import com.example.lms.repository.SubmissionRepository;
import com.example.lms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    @Mock private SubmissionRepository submissionRepository;
    @Mock private AssignmentRepository assignmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private ClassSecurityService classSecurityService;
    @Mock private FileStorageServiceImpl fileStorageService;

    @InjectMocks private SubmissionService submissionService;

    private UUID classId;
    private UserEntity owner;
    private UserEntity teacher;
    private UserEntity student;
    private ClassMemberEntity ownerMember;
    private ClassMemberEntity teacherMember;
    private ClassMemberEntity studentMember;
    private AssignmentEntity assignment;

    @BeforeEach
    void setUp() {
        classId = UUID.randomUUID();

        owner = UserEntity.builder()
                .id(UUID.randomUUID())
                .firstName("Owner").lastName("User")
                .email("owner@test.com").passwordHash("hash")
                .build();
        teacher = UserEntity.builder()
                .id(UUID.randomUUID())
                .firstName("Teacher").lastName("User")
                .email("teacher@test.com").passwordHash("hash")
                .build();
        student = UserEntity.builder()
                .id(UUID.randomUUID())
                .firstName("Student").lastName("User")
                .email("student@test.com").passwordHash("hash")
                .build();

        ownerMember = ClassMemberEntity.builder()
                .classId(classId).userId(owner.getId()).role(Role.OWNER).build();
        teacherMember = ClassMemberEntity.builder()
                .classId(classId).userId(teacher.getId()).role(Role.TEACHER).build();
        studentMember = ClassMemberEntity.builder()
                .classId(classId).userId(student.getId()).role(Role.STUDENT).build();

        assignment = AssignmentEntity.builder()
                .id(UUID.randomUUID())
                .classId(classId)
                .title("Homework 1")
                .description("Solve problems")
                .createdBy(teacher.getId())
                .createdAt(Instant.now())
                .build();
    }

    // --- submit ---

    @Test
    void should_createSubmission_whenStudentSubmitsFirstTime() {
        // Given
        when(assignmentRepository.findById(assignment.getId())).thenReturn(Optional.of(assignment));
        when(classSecurityService.requireMember(classId, student.getId())).thenReturn(studentMember);
        when(submissionRepository.findByAssignmentIdAndStudentId(assignment.getId(), student.getId()))
                .thenReturn(Optional.empty());
        when(submissionRepository.save(any())).thenAnswer(inv -> {
            SubmissionEntity e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });
        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));

        // When
        SubmissionDto result = submissionService.submit(assignment.getId(), "My answer", null, student);

        // Then
        assertThat(result.answerText()).isEqualTo("My answer");
        assertThat(result.studentId()).isEqualTo(student.getId());
        assertThat(result.studentName()).isEqualTo("Student User");
        assertThat(result.grade()).isNull();
        verify(submissionRepository).save(any());
    }

    @Test
    void should_upsertSubmission_whenStudentResubmits() {
        // Given
        SubmissionEntity existing = SubmissionEntity.builder()
                .id(UUID.randomUUID())
                .assignmentId(assignment.getId())
                .studentId(student.getId())
                .answerText("Old answer")
                .submittedAt(Instant.now())
                .build();

        when(assignmentRepository.findById(assignment.getId())).thenReturn(Optional.of(assignment));
        when(classSecurityService.requireMember(classId, student.getId())).thenReturn(studentMember);
        when(submissionRepository.findByAssignmentIdAndStudentId(assignment.getId(), student.getId()))
                .thenReturn(Optional.of(existing));
        when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));

        // When
        SubmissionDto result = submissionService.submit(assignment.getId(), "Updated answer", null, student);

        // Then
        assertThat(result.answerText()).isEqualTo("Updated answer");
        assertThat(result.id()).isEqualTo(existing.getId());
        verify(submissionRepository).save(argThat(s -> s.getAnswerText().equals("Updated answer")));
    }

    @Test
    void should_storeFile_whenStudentSubmitsWithFile() {
        // Given
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "content".getBytes());

        when(assignmentRepository.findById(assignment.getId())).thenReturn(Optional.of(assignment));
        when(classSecurityService.requireMember(classId, student.getId())).thenReturn(studentMember);
        when(submissionRepository.findByAssignmentIdAndStudentId(assignment.getId(), student.getId()))
                .thenReturn(Optional.empty());
        when(fileStorageService.store(file)).thenReturn("uuid-file.pdf");
        when(submissionRepository.save(any())).thenAnswer(inv -> {
            SubmissionEntity e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });
        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));

        // When
        SubmissionDto result = submissionService.submit(assignment.getId(), null, file, student);

        // Then
        assertThat(result.fileUrl()).isEqualTo("/api/v1/files/uuid-file.pdf");
        verify(fileStorageService).store(file);
    }

    @Test
    void should_throwForbidden_whenNonStudentTriesToSubmit() {
        // Given
        when(assignmentRepository.findById(assignment.getId())).thenReturn(Optional.of(assignment));
        when(classSecurityService.requireMember(classId, teacher.getId())).thenReturn(teacherMember);

        // When / Then
        assertThatThrownBy(() -> submissionService.submit(assignment.getId(), "answer", null, teacher))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void should_throw404_whenAssignmentNotFound_onSubmit() {
        // Given
        when(assignmentRepository.findById(any())).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> submissionService.submit(UUID.randomUUID(), "answer", null, student))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- getSubmissions (TEACHER/OWNER) ---

    @Test
    void should_returnAllSubmissions_whenTeacher() {
        // Given
        SubmissionEntity sub = SubmissionEntity.builder()
                .id(UUID.randomUUID())
                .assignmentId(assignment.getId())
                .studentId(student.getId())
                .answerText("Answer")
                .submittedAt(Instant.now())
                .build();

        when(assignmentRepository.findById(assignment.getId())).thenReturn(Optional.of(assignment));
        when(classSecurityService.requireOwnerOrTeacher(classId, teacher.getId())).thenReturn(teacherMember);
        when(submissionRepository.findAllByAssignmentId(assignment.getId())).thenReturn(List.of(sub));
        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));

        // When
        List<SubmissionDto> result = submissionService.getSubmissions(assignment.getId(), teacher.getId());

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).studentName()).isEqualTo("Student User");
        assertThat(result.get(0).answerText()).isEqualTo("Answer");
    }

    @Test
    void should_throwForbidden_whenStudentTriesToGetAllSubmissions() {
        // Given
        when(assignmentRepository.findById(assignment.getId())).thenReturn(Optional.of(assignment));
        when(classSecurityService.requireOwnerOrTeacher(classId, student.getId()))
                .thenThrow(new ForbiddenException("OWNER or TEACHER required"));

        // When / Then
        assertThatThrownBy(() -> submissionService.getSubmissions(assignment.getId(), student.getId()))
                .isInstanceOf(ForbiddenException.class);
    }

    // --- getMySubmission (STUDENT) ---

    @Test
    void should_returnMySubmission_whenStudentHasSubmitted() {
        // Given
        SubmissionEntity sub = SubmissionEntity.builder()
                .id(UUID.randomUUID())
                .assignmentId(assignment.getId())
                .studentId(student.getId())
                .answerText("My answer")
                .grade((short) 85)
                .submittedAt(Instant.now())
                .build();

        when(assignmentRepository.findById(assignment.getId())).thenReturn(Optional.of(assignment));
        when(classSecurityService.requireMember(classId, student.getId())).thenReturn(studentMember);
        when(submissionRepository.findByAssignmentIdAndStudentId(assignment.getId(), student.getId()))
                .thenReturn(Optional.of(sub));
        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));

        // When
        SubmissionDto result = submissionService.getMySubmission(assignment.getId(), student.getId());

        // Then
        assertThat(result.answerText()).isEqualTo("My answer");
        assertThat(result.grade()).isEqualTo(85);
        assertThat(result.studentName()).isEqualTo("Student User");
    }

    @Test
    void should_throw404_whenStudentHasNoSubmission() {
        // Given
        when(assignmentRepository.findById(assignment.getId())).thenReturn(Optional.of(assignment));
        when(classSecurityService.requireMember(classId, student.getId())).thenReturn(studentMember);
        when(submissionRepository.findByAssignmentIdAndStudentId(assignment.getId(), student.getId()))
                .thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> submissionService.getMySubmission(assignment.getId(), student.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- grade ---

    @Test
    void should_grade_whenInRange() {
        // Given
        SubmissionEntity sub = SubmissionEntity.builder()
                .id(UUID.randomUUID())
                .assignmentId(assignment.getId())
                .studentId(student.getId())
                .answerText("Answer")
                .submittedAt(Instant.now())
                .build();

        when(submissionRepository.findById(sub.getId())).thenReturn(Optional.of(sub));
        when(assignmentRepository.findById(assignment.getId())).thenReturn(Optional.of(assignment));
        when(classSecurityService.requireOwnerOrTeacher(classId, teacher.getId())).thenReturn(teacherMember);
        when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));

        // When
        SubmissionDto result = submissionService.grade(sub.getId(), new GradeRequest(95), teacher.getId());

        // Then
        assertThat(result.grade()).isEqualTo(95);
        verify(submissionRepository).save(argThat(s ->
                s.getGrade() == 95 && s.getGradedAt() != null));
    }

    @Test
    void should_throw404_whenSubmissionNotFound_onGrade() {
        // Given
        when(submissionRepository.findById(any())).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> submissionService.grade(UUID.randomUUID(), new GradeRequest(50), teacher.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void should_throwForbidden_whenStudentTriesToGrade() {
        // Given
        SubmissionEntity sub = SubmissionEntity.builder()
                .id(UUID.randomUUID())
                .assignmentId(assignment.getId())
                .studentId(student.getId())
                .answerText("Answer")
                .submittedAt(Instant.now())
                .build();

        when(submissionRepository.findById(sub.getId())).thenReturn(Optional.of(sub));
        when(assignmentRepository.findById(assignment.getId())).thenReturn(Optional.of(assignment));
        when(classSecurityService.requireOwnerOrTeacher(classId, student.getId()))
                .thenThrow(new ForbiddenException("OWNER or TEACHER required"));

        // When / Then
        assertThatThrownBy(() -> submissionService.grade(sub.getId(), new GradeRequest(50), student.getId()))
                .isInstanceOf(ForbiddenException.class);
    }
}

package com.example.lms.service;

import com.example.lms.dto.AssignmentDto;
import com.example.lms.dto.AssignmentDetailDto;
import com.example.lms.dto.CreateAssignmentRequest;
import com.example.lms.entity.AssignmentEntity;
import com.example.lms.entity.ClassMemberEntity;
import com.example.lms.entity.Role;
import com.example.lms.entity.SubmissionEntity;
import com.example.lms.entity.UserEntity;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

    @Mock private AssignmentRepository  assignmentRepository;
    @Mock private SubmissionRepository  submissionRepository;
    @Mock private UserRepository        userRepository;
    @Mock private ClassSecurityService  classSecurityService;
    @InjectMocks private AssignmentService assignmentService;

    private UUID            classId;
    private UserEntity      owner;
    private UserEntity      teacher;
    private UserEntity      student;
    private ClassMemberEntity ownerMember;
    private ClassMemberEntity teacherMember;
    private ClassMemberEntity studentMember;
    private AssignmentEntity assignment;

    @BeforeEach
    void setUp() {
        classId = UUID.randomUUID();
        owner = UserEntity.builder()
                .id(UUID.randomUUID())
                .email("owner@test.com")
                .firstName("Owner").lastName("User")
                .passwordHash("hash")
                .build();
        teacher = UserEntity.builder()
                .id(UUID.randomUUID())
                .email("teacher@test.com")
                .firstName("Teacher").lastName("User")
                .passwordHash("hash")
                .build();
        student = UserEntity.builder()
                .id(UUID.randomUUID())
                .email("student@test.com")
                .firstName("Student").lastName("User")
                .passwordHash("hash")
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

    @Test
    void should_returnAssignmentsWithSubmissionStatus_whenStudent() {
        // Given
        when(classSecurityService.requireMember(classId, student.getId())).thenReturn(studentMember);
        when(assignmentRepository.findAllByClassIdOrderByCreatedAtDesc(classId))
                .thenReturn(List.of(assignment));
        when(submissionRepository.findByAssignmentIdAndStudentId(assignment.getId(), student.getId()))
                .thenReturn(Optional.of(SubmissionEntity.builder()
                        .grade((short) 85)
                        .submittedAt(Instant.now())
                        .build()));

        // When
        List<AssignmentDto> result = assignmentService.getAssignments(classId, student.getId());

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Homework 1");
        assertThat(result.get(0).submissionStatus()).isEqualTo("GRADED");
        assertThat(result.get(0).grade()).isEqualTo(85);
    }

    @Test
    void should_returnSubmittedStatus_whenStudentSubmittedButNotGraded() {
        // Given
        when(classSecurityService.requireMember(classId, student.getId())).thenReturn(studentMember);
        when(assignmentRepository.findAllByClassIdOrderByCreatedAtDesc(classId))
                .thenReturn(List.of(assignment));
        when(submissionRepository.findByAssignmentIdAndStudentId(assignment.getId(), student.getId()))
                .thenReturn(Optional.of(SubmissionEntity.builder()
                        .grade(null)
                        .submittedAt(Instant.now())
                        .build()));

        List<AssignmentDto> result = assignmentService.getAssignments(classId, student.getId());

        assertThat(result.get(0).submissionStatus()).isEqualTo("SUBMITTED");
        assertThat(result.get(0).grade()).isNull();
    }

    @Test
    void should_returnAssignmentsWithNotSubmittedStatus_whenStudentHasNoSubmission() {
        // Given
        when(classSecurityService.requireMember(classId, student.getId())).thenReturn(studentMember);
        when(assignmentRepository.findAllByClassIdOrderByCreatedAtDesc(classId))
                .thenReturn(List.of(assignment));
        when(submissionRepository.findByAssignmentIdAndStudentId(assignment.getId(), student.getId()))
                .thenReturn(Optional.empty());

        // When
        List<AssignmentDto> result = assignmentService.getAssignments(classId, student.getId());

        // Then
        assertThat(result.get(0).submissionStatus()).isEqualTo("NOT_SUBMITTED");
        assertThat(result.get(0).grade()).isNull();
    }

    @Test
    void should_returnAssignmentsWithoutSubmissionStatus_whenTeacher() {
        // Given
        when(classSecurityService.requireMember(classId, teacher.getId())).thenReturn(teacherMember);
        when(assignmentRepository.findAllByClassIdOrderByCreatedAtDesc(classId))
                .thenReturn(List.of(assignment));

        // When
        List<AssignmentDto> result = assignmentService.getAssignments(classId, teacher.getId());

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).submissionStatus()).isNull();
        assertThat(result.get(0).grade()).isNull();
    }

    @Test
    void should_throwForbidden_whenNotMember_onGetAssignments() {
        when(classSecurityService.requireMember(classId, student.getId()))
                .thenThrow(new ForbiddenException("Not a member"));

        assertThatThrownBy(() -> assignmentService.getAssignments(classId, student.getId()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void should_createAssignment_whenTeacher() {
        // Given
        when(classSecurityService.requireOwnerOrTeacher(classId, teacher.getId()))
                .thenReturn(teacherMember);
        when(assignmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        AssignmentDto result = assignmentService.createAssignment(classId,
                new CreateAssignmentRequest("Test Assignment", "Description"), teacher);

        // Then
        verify(assignmentRepository).save(argThat(a ->
                a.getTitle().equals("Test Assignment")
                        && a.getDescription().equals("Description")
                        && a.getCreatedBy().equals(teacher.getId())
                        && a.getClassId().equals(classId)));
        assertThat(result.title()).isEqualTo("Test Assignment");
        assertThat(result.submissionStatus()).isNull();
    }

    @Test
    void should_createAssignment_whenOwner() {
        when(classSecurityService.requireOwnerOrTeacher(classId, owner.getId()))
                .thenReturn(ownerMember);
        when(assignmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AssignmentDto result = assignmentService.createAssignment(classId,
                new CreateAssignmentRequest("Owner Assignment", null), owner);

        assertThat(result.title()).isEqualTo("Owner Assignment");
        verify(assignmentRepository).save(any());
    }

    @Test
    void should_throwForbidden_whenStudent_onCreateAssignment() {
        when(classSecurityService.requireOwnerOrTeacher(classId, student.getId()))
                .thenThrow(new ForbiddenException("OWNER or TEACHER required"));

        assertThatThrownBy(() -> assignmentService.createAssignment(classId,
                new CreateAssignmentRequest("Title", null), student))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void should_returnAssignmentDetail_whenMember() {
        when(assignmentRepository.findById(assignment.getId())).thenReturn(Optional.of(assignment));
        when(classSecurityService.requireMember(classId, student.getId())).thenReturn(studentMember);
        when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
        when(submissionRepository.findByAssignmentIdAndStudentId(assignment.getId(), student.getId()))
                .thenReturn(Optional.empty());

        AssignmentDetailDto result = assignmentService.getAssignment(assignment.getId(), student.getId());

        assertThat(result.title()).isEqualTo("Homework 1");
        assertThat(result.createdByName()).isEqualTo("Teacher User");
        assertThat(result.submissionStatus()).isEqualTo("NOT_SUBMITTED");
    }

    @Test
    void should_throw404_whenAssignmentNotFound() {
        when(assignmentRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assignmentService.getAssignment(UUID.randomUUID(), student.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void should_throwForbidden_whenNotMember_onGetAssignment() {
        when(assignmentRepository.findById(assignment.getId())).thenReturn(Optional.of(assignment));
        when(classSecurityService.requireMember(classId, student.getId()))
                .thenThrow(new ForbiddenException("Not a member"));

        assertThatThrownBy(() -> assignmentService.getAssignment(assignment.getId(), student.getId()))
                .isInstanceOf(ForbiddenException.class);
    }
}

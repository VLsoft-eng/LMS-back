package com.example.lms.service;

import com.example.lms.dto.AddCommentRequest;
import com.example.lms.dto.CommentDto;
import com.example.lms.entity.AssignmentEntity;
import com.example.lms.entity.ClassMemberEntity;
import com.example.lms.entity.CommentEntity;
import com.example.lms.entity.Role;
import com.example.lms.entity.UserEntity;
import com.example.lms.exception.ForbiddenException;
import com.example.lms.exception.ResourceNotFoundException;
import com.example.lms.repository.AssignmentRepository;
import com.example.lms.repository.CommentRepository;
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
class CommentServiceTest {

    @Mock private CommentRepository     commentRepository;
    @Mock private AssignmentRepository  assignmentRepository;
    @Mock private UserRepository        userRepository;
    @Mock private ClassSecurityService  classSecurityService;
    @InjectMocks private CommentService commentService;

    private UUID              classId;
    private UserEntity        owner;
    private UserEntity        student;
    private ClassMemberEntity ownerMember;
    private ClassMemberEntity studentMember;
    private AssignmentEntity  assignment;

    @BeforeEach
    void setUp() {
        classId = UUID.randomUUID();

        owner = UserEntity.builder()
                .id(UUID.randomUUID())
                .email("owner@test.com")
                .firstName("Owner").lastName("User")
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
        studentMember = ClassMemberEntity.builder()
                .classId(classId).userId(student.getId()).role(Role.STUDENT).build();

        assignment = AssignmentEntity.builder()
                .id(UUID.randomUUID())
                .classId(classId)
                .title("Homework 1")
                .createdBy(owner.getId())
                .createdAt(Instant.now())
                .build();
    }

    // ── getComments ────────────────────────────────────────────────

    @Test
    void should_returnComments_whenMember() {
        // Given
        when(assignmentRepository.findById(assignment.getId())).thenReturn(Optional.of(assignment));
        when(classSecurityService.requireMember(classId, owner.getId())).thenReturn(ownerMember);

        CommentEntity comment = CommentEntity.builder()
                .id(UUID.randomUUID())
                .assignmentId(assignment.getId())
                .authorId(student.getId())
                .text("Great task!")
                .createdAt(Instant.now())
                .build();
        when(commentRepository.findAllByAssignmentIdOrderByCreatedAtAsc(assignment.getId()))
                .thenReturn(List.of(comment));
        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));

        // When
        List<CommentDto> result = commentService.getComments(assignment.getId(), owner.getId());

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).text()).isEqualTo("Great task!");
        assertThat(result.get(0).authorName()).isEqualTo("Student User");
        assertThat(result.get(0).assignmentId()).isEqualTo(assignment.getId());
        assertThat(result.get(0).authorId()).isEqualTo(student.getId());
    }

    @Test
    void should_returnEmptyList_whenNoComments() {
        // Given
        when(assignmentRepository.findById(assignment.getId())).thenReturn(Optional.of(assignment));
        when(classSecurityService.requireMember(classId, owner.getId())).thenReturn(ownerMember);
        when(commentRepository.findAllByAssignmentIdOrderByCreatedAtAsc(assignment.getId()))
                .thenReturn(List.of());

        // When
        List<CommentDto> result = commentService.getComments(assignment.getId(), owner.getId());

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void should_throw404_whenAssignmentNotFound_onGetComments() {
        // Given
        when(assignmentRepository.findById(any())).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> commentService.getComments(UUID.randomUUID(), owner.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void should_throwForbidden_whenNotMember_onGetComments() {
        // Given
        when(assignmentRepository.findById(assignment.getId())).thenReturn(Optional.of(assignment));
        when(classSecurityService.requireMember(classId, student.getId()))
                .thenThrow(new ForbiddenException("Not a member"));

        // When / Then
        assertThatThrownBy(() -> commentService.getComments(assignment.getId(), student.getId()))
                .isInstanceOf(ForbiddenException.class);
    }

    // ── addComment ─────────────────────────────────────────────────

    @Test
    void should_addComment_whenMember() {
        // Given
        when(assignmentRepository.findById(assignment.getId())).thenReturn(Optional.of(assignment));
        when(classSecurityService.requireMember(classId, owner.getId())).thenReturn(ownerMember);
        when(commentRepository.save(any())).thenAnswer(inv -> {
            CommentEntity e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        // When
        CommentDto result = commentService.addComment(
                assignment.getId(),
                new AddCommentRequest("Nice work!"),
                owner
        );

        // Then
        assertThat(result.text()).isEqualTo("Nice work!");
        assertThat(result.authorId()).isEqualTo(owner.getId());
        assertThat(result.authorName()).isEqualTo("Owner User");
        assertThat(result.assignmentId()).isEqualTo(assignment.getId());
        assertThat(result.id()).isNotNull();

        verify(commentRepository).save(argThat(c ->
                c.getAssignmentId().equals(assignment.getId())
                        && c.getAuthorId().equals(owner.getId())
                        && c.getText().equals("Nice work!")
        ));
    }

    @Test
    void should_addComment_whenStudent() {
        // Given
        when(assignmentRepository.findById(assignment.getId())).thenReturn(Optional.of(assignment));
        when(classSecurityService.requireMember(classId, student.getId())).thenReturn(studentMember);
        when(commentRepository.save(any())).thenAnswer(inv -> {
            CommentEntity e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        // When
        CommentDto result = commentService.addComment(
                assignment.getId(),
                new AddCommentRequest("I have a question"),
                student
        );

        // Then
        assertThat(result.text()).isEqualTo("I have a question");
        assertThat(result.authorId()).isEqualTo(student.getId());
        assertThat(result.authorName()).isEqualTo("Student User");
    }

    @Test
    void should_throw404_whenAssignmentNotFound_onAddComment() {
        // Given
        when(assignmentRepository.findById(any())).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> commentService.addComment(
                UUID.randomUUID(), new AddCommentRequest("text"), owner))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void should_throwForbidden_whenNotMember_onAddComment() {
        // Given
        when(assignmentRepository.findById(assignment.getId())).thenReturn(Optional.of(assignment));
        when(classSecurityService.requireMember(classId, student.getId()))
                .thenThrow(new ForbiddenException("Not a member"));

        // When / Then
        assertThatThrownBy(() -> commentService.addComment(
                assignment.getId(), new AddCommentRequest("text"), student))
                .isInstanceOf(ForbiddenException.class);
    }
}

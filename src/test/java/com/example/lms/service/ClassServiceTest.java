package com.example.lms.service;

import com.example.lms.dto.ClassDto;
import com.example.lms.dto.CreateClassRequest;
import com.example.lms.dto.UpdateClassRequest;
import com.example.lms.entity.ClassEntity;
import com.example.lms.entity.ClassMemberEntity;
import com.example.lms.entity.Role;
import com.example.lms.entity.UserEntity;
import com.example.lms.exception.ConflictException;
import com.example.lms.exception.ForbiddenException;
import com.example.lms.exception.ResourceNotFoundException;
import com.example.lms.repository.ClassMemberRepository;
import com.example.lms.repository.ClassRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TICKET-BE-07 / TICKET-BE-16: Unit tests for ClassService.
 * Tests follow the AAA (Given / When / Then) pattern.
 * All repositories and ClassSecurityService are mocked via @Mock.
 */
@ExtendWith(MockitoExtension.class)
class ClassServiceTest {

    @Mock private ClassRepository       classRepository;
    @Mock private ClassMemberRepository classMemberRepository;
    @Mock private ClassSecurityService  classSecurityService;
    @InjectMocks private ClassService   classService;

    private UserEntity        user;
    private ClassEntity       cls;
    private ClassMemberEntity ownerMember;

    @BeforeEach
    void setUp() {
        user = UserEntity.builder()
                .id(UUID.randomUUID())
                .email("user@test.com")
                .firstName("Ivan").lastName("Ivanov")
                .passwordHash("hash")
                .build();

        cls = ClassEntity.builder()
                .id(UUID.randomUUID())
                .name("Math")
                .code("MATH0001")
                .ownerId(user.getId())
                .build();

        ownerMember = ClassMemberEntity.builder()
                .id(UUID.randomUUID())
                .classId(cls.getId())
                .userId(user.getId())
                .role(Role.OWNER)
                .build();
    }

    // ─── createClass ─────────────────────────────────────────────────────────

    @Test
    void should_createClass_andAddOwnerAsMember() {
        // Given
        when(classRepository.existsByCode(anyString())).thenReturn(false);
        when(classRepository.save(any())).thenReturn(cls);
        when(classMemberRepository.save(any())).thenReturn(ownerMember);
        when(classMemberRepository.countByClassId(cls.getId())).thenReturn(1L);

        // When
        ClassDto result = classService.createClass(new CreateClassRequest("Math"), user);

        // Then
        verify(classRepository).save(any(ClassEntity.class));
        verify(classMemberRepository).save(argThat(
                m -> m.getRole() == Role.OWNER && m.getUserId().equals(user.getId())));
        assertThat(result.name()).isEqualTo("Math");
        assertThat(result.myRole()).isEqualTo(Role.OWNER);
        assertThat(result.memberCount()).isEqualTo(1);
    }

    // ─── getMyClasses ─────────────────────────────────────────────────────────

    @Test
    void should_returnMyClasses_withMyRole() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        when(classRepository.findAllByMembersUserId(user.getId(), pageable))
                .thenReturn(new PageImpl<>(List.of(cls), pageable, 1));
        when(classMemberRepository.findByClassIdAndUserId(cls.getId(), user.getId()))
                .thenReturn(Optional.of(ownerMember));
        when(classMemberRepository.countByClassId(cls.getId())).thenReturn(1L);

        // When
        Page<ClassDto> result = classService.getMyClasses(user.getId(), pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("Math");
        assertThat(result.getContent().get(0).myRole()).isEqualTo(Role.OWNER);
        assertThat(result.getContent().get(0).memberCount()).isEqualTo(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    // ─── joinClass ────────────────────────────────────────────────────────────

    @Test
    void should_joinClass_whenCodeValid_andAddStudentRole() {
        // Given
        UserEntity student = UserEntity.builder().id(UUID.randomUUID()).build();
        ClassMemberEntity studentMember = ClassMemberEntity.builder()
                .classId(cls.getId()).userId(student.getId()).role(Role.STUDENT).build();

        when(classRepository.findByCode("MATH0001")).thenReturn(Optional.of(cls));
        when(classMemberRepository.findByClassIdAndUserId(cls.getId(), student.getId()))
                .thenReturn(Optional.empty());
        when(classMemberRepository.save(any())).thenReturn(studentMember);
        when(classMemberRepository.countByClassId(cls.getId())).thenReturn(2L);

        // When
        ClassDto result = classService.joinClass("MATH0001", student);

        // Then
        verify(classMemberRepository).save(argThat(
                m -> m.getRole() == Role.STUDENT && m.getUserId().equals(student.getId())));
        assertThat(result.name()).isEqualTo("Math");
        assertThat(result.myRole()).isEqualTo(Role.STUDENT);
    }

    @Test
    void should_throw404_whenCodeNotFound() {
        // Given
        when(classRepository.findByCode(anyString())).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> classService.joinClass("NOTEXIST", user))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void should_throwConflict_whenAlreadyMember() {
        // Given
        when(classRepository.findByCode("MATH0001")).thenReturn(Optional.of(cls));
        when(classMemberRepository.findByClassIdAndUserId(cls.getId(), user.getId()))
                .thenReturn(Optional.of(ownerMember));

        // When / Then
        assertThatThrownBy(() -> classService.joinClass("MATH0001", user))
                .isInstanceOf(ConflictException.class);
    }

    // ─── updateClass ─────────────────────────────────────────────────────────

    @Test
    void should_updateClassName_whenOwner() {
        // Given
        when(classSecurityService.requireOwner(cls.getId(), user.getId()))
                .thenReturn(ownerMember);
        ClassEntity renamed = ClassEntity.builder()
                .id(cls.getId()).name("New Name").code(cls.getCode()).ownerId(user.getId()).build();
        when(classRepository.findById(cls.getId())).thenReturn(Optional.of(cls));
        when(classRepository.save(any())).thenReturn(renamed);
        when(classMemberRepository.countByClassId(cls.getId())).thenReturn(1L);

        // When
        ClassDto result = classService.updateClass(cls.getId(), new UpdateClassRequest("New Name"), user.getId());

        // Then
        assertThat(result.name()).isEqualTo("New Name");
        assertThat(result.myRole()).isEqualTo(Role.OWNER);
    }

    @Test
    void should_throwForbidden_whenNotOwner_onUpdate() {
        // Given — classSecurityService.requireOwner() throws for non-OWNER
        UserEntity other = UserEntity.builder().id(UUID.randomUUID()).build();
        when(classSecurityService.requireOwner(cls.getId(), other.getId()))
                .thenThrow(new ForbiddenException("Not the owner"));

        // When / Then
        assertThatThrownBy(() -> classService.updateClass(cls.getId(), new UpdateClassRequest("New Name"), other.getId()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void should_throw404_whenClassNotFound_onUpdate() {
        // Given — requireOwner passes but class not found (or requireOwner itself triggers 404)
        when(classSecurityService.requireOwner(any(), any()))
                .thenThrow(new ResourceNotFoundException("Class not found"));

        // When / Then
        assertThatThrownBy(() -> classService.updateClass(UUID.randomUUID(), new UpdateClassRequest("New Name"), user.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── deleteClass ─────────────────────────────────────────────────────────

    @Test
    void should_deleteClass_whenOwner() {
        // Given
        when(classSecurityService.requireOwner(cls.getId(), user.getId()))
                .thenReturn(ownerMember);
        when(classRepository.findById(cls.getId())).thenReturn(Optional.of(cls));

        // When
        classService.deleteClass(cls.getId(), user.getId());

        // Then
        verify(classRepository).delete(cls);
    }

    @Test
    void should_throwForbidden_whenNotOwner_onDelete() {
        // Given
        UserEntity other = UserEntity.builder().id(UUID.randomUUID()).build();
        when(classSecurityService.requireOwner(cls.getId(), other.getId()))
                .thenThrow(new ForbiddenException("Not the owner"));

        // When / Then
        assertThatThrownBy(() -> classService.deleteClass(cls.getId(), other.getId()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void should_throw404_whenClassNotFound_onDelete() {
        // Given
        when(classSecurityService.requireOwner(any(), any()))
                .thenThrow(new ResourceNotFoundException("Class not found"));

        // When / Then
        assertThatThrownBy(() -> classService.deleteClass(UUID.randomUUID(), user.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── getClass ──────────────────────────────────────────────────────────────

    @Test
    void should_returnClass_whenMember() {
        // Given
        when(classSecurityService.requireMember(cls.getId(), user.getId()))
                .thenReturn(ownerMember);
        when(classRepository.findById(cls.getId())).thenReturn(Optional.of(cls));
        when(classMemberRepository.countByClassId(cls.getId())).thenReturn(1L);

        // When
        ClassDto result = classService.getClass(cls.getId(), user.getId());

        // Then
        assertThat(result.id()).isEqualTo(cls.getId());
        assertThat(result.name()).isEqualTo("Math");
        assertThat(result.code()).isEqualTo("MATH0001");
        assertThat(result.myRole()).isEqualTo(Role.OWNER);
        assertThat(result.memberCount()).isEqualTo(1);
    }

    @Test
    void should_throw404_whenClassNotFound_onGetClass() {
        // Given
        UUID unknownId = UUID.randomUUID();
        when(classSecurityService.requireMember(unknownId, user.getId()))
                .thenThrow(new ResourceNotFoundException("Class not found: " + unknownId));

        // When / Then
        assertThatThrownBy(() -> classService.getClass(unknownId, user.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void should_throwForbidden_whenNotMember_onGetClass() {
        // Given
        UserEntity nonMember = UserEntity.builder().id(UUID.randomUUID()).build();
        when(classSecurityService.requireMember(cls.getId(), nonMember.getId()))
                .thenThrow(new ForbiddenException("Not a member of class: " + cls.getId()));

        // When / Then
        assertThatThrownBy(() -> classService.getClass(cls.getId(), nonMember.getId()))
                .isInstanceOf(ForbiddenException.class);
    }

    // ─── getClassCode ─────────────────────────────────────────────────────────

    @Test
    void should_returnCode_whenOwnerOrTeacher() {
        // Given
        when(classSecurityService.requireOwnerOrTeacher(cls.getId(), user.getId()))
                .thenReturn(ownerMember);
        when(classRepository.findById(cls.getId())).thenReturn(Optional.of(cls));

        // When
        String result = classService.getClassCode(cls.getId(), user.getId());

        // Then
        assertThat(result).isEqualTo("MATH0001");
    }

    @Test
    void should_throwForbidden_whenStudent_onGetCode() {
        // Given — classSecurityService.requireOwnerOrTeacher() throws for STUDENT
        UserEntity student = UserEntity.builder().id(UUID.randomUUID()).build();
        when(classSecurityService.requireOwnerOrTeacher(cls.getId(), student.getId()))
                .thenThrow(new ForbiddenException("Insufficient role"));

        // When / Then
        assertThatThrownBy(() -> classService.getClassCode(cls.getId(), student.getId()))
                .isInstanceOf(ForbiddenException.class);
    }
}

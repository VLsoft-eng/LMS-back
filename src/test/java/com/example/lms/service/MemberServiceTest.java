package com.example.lms.service;

import com.example.lms.dto.AssignRoleRequest;
import com.example.lms.dto.MemberDto;
import com.example.lms.entity.ClassMemberEntity;
import com.example.lms.entity.Role;
import com.example.lms.entity.UserEntity;
import com.example.lms.exception.ForbiddenException;
import com.example.lms.exception.ResourceNotFoundException;
import com.example.lms.repository.ClassMemberRepository;
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
class MemberServiceTest {

    @Mock private ClassMemberRepository classMemberRepository;
    @Mock private UserRepository        userRepository;
    @Mock private ClassSecurityService  classSecurityService;
    @InjectMocks private MemberService  memberService;

    private UUID           classId;
    private UserEntity     owner;
    private UserEntity     teacher;
    private UserEntity     student;
    private ClassMemberEntity ownerMember;
    private ClassMemberEntity teacherMember;
    private ClassMemberEntity studentMember;

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
                .id(UUID.randomUUID())
                .classId(classId)
                .userId(owner.getId())
                .role(Role.OWNER)
                .joinedAt(Instant.now())
                .build();
        teacherMember = ClassMemberEntity.builder()
                .id(UUID.randomUUID())
                .classId(classId)
                .userId(teacher.getId())
                .role(Role.TEACHER)
                .joinedAt(Instant.now())
                .build();
        studentMember = ClassMemberEntity.builder()
                .id(UUID.randomUUID())
                .classId(classId)
                .userId(student.getId())
                .role(Role.STUDENT)
                .joinedAt(Instant.now())
                .build();
    }

    @Test
    void should_returnMembers_whenMemberOfClass() {
        // Given
        when(classSecurityService.requireMember(classId, owner.getId())).thenReturn(ownerMember);
        when(classMemberRepository.findAllByClassIdOrderByJoinedAtAsc(classId))
                .thenReturn(List.of(ownerMember, teacherMember, studentMember));
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));

        // When
        List<MemberDto> result = memberService.getMembers(classId, owner.getId());

        // Then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).userId()).isEqualTo(owner.getId());
        assertThat(result.get(0).firstName()).isEqualTo("Owner");
        assertThat(result.get(0).role()).isEqualTo("OWNER");
        assertThat(result.get(1).userId()).isEqualTo(teacher.getId());
        assertThat(result.get(1).role()).isEqualTo("TEACHER");
        assertThat(result.get(2).userId()).isEqualTo(student.getId());
        assertThat(result.get(2).role()).isEqualTo("STUDENT");
    }

    @Test
    void should_throwForbidden_whenNotMember_onGetMembers() {
        // Given
        when(classSecurityService.requireMember(classId, owner.getId()))
                .thenThrow(new ForbiddenException("Not a member of class"));

        // When / Then
        assertThatThrownBy(() -> memberService.getMembers(classId, owner.getId()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void should_assignRole_whenOwnerAndTargetNotOwner() {
        // Given
        when(classSecurityService.requireOwner(classId, owner.getId())).thenReturn(ownerMember);
        when(classMemberRepository.findByClassIdAndUserId(classId, student.getId()))
                .thenReturn(Optional.of(studentMember));
        ClassMemberEntity updated = ClassMemberEntity.builder()
                .id(studentMember.getId())
                .classId(classId)
                .userId(student.getId())
                .role(Role.TEACHER)
                .joinedAt(studentMember.getJoinedAt())
                .build();
        when(classMemberRepository.save(any())).thenReturn(updated);
        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));

        // When
        MemberDto result = memberService.assignRole(classId, student.getId(),
                new AssignRoleRequest("TEACHER"), owner.getId());

        // Then
        verify(classMemberRepository).save(argThat(m ->
                m.getUserId().equals(student.getId()) && m.getRole() == Role.TEACHER));
        assertThat(result.role()).isEqualTo("TEACHER");
    }

    @Test
    void should_throwForbidden_whenNotOwner_onAssignRole() {
        // Given
        when(classSecurityService.requireOwner(classId, student.getId()))
                .thenThrow(new ForbiddenException("OWNER role required"));

        // When / Then
        assertThatThrownBy(() -> memberService.assignRole(classId, teacher.getId(),
                new AssignRoleRequest("STUDENT"), student.getId()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void should_throwForbidden_whenChangingOwnerRole() {
        // Given
        when(classSecurityService.requireOwner(classId, owner.getId())).thenReturn(ownerMember);
        when(classMemberRepository.findByClassIdAndUserId(classId, owner.getId()))
                .thenReturn(Optional.of(ownerMember));

        // When / Then
        assertThatThrownBy(() -> memberService.assignRole(classId, owner.getId(),
                new AssignRoleRequest("TEACHER"), owner.getId()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void should_throw404_whenMemberNotFound_onAssignRole() {
        // Given
        when(classSecurityService.requireOwner(classId, owner.getId())).thenReturn(ownerMember);
        when(classMemberRepository.findByClassIdAndUserId(classId, student.getId()))
                .thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> memberService.assignRole(classId, student.getId(),
                new AssignRoleRequest("TEACHER"), owner.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

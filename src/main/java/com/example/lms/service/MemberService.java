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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final ClassMemberRepository classMemberRepository;
    private final UserRepository        userRepository;
    private final ClassSecurityService  classSecurityService;

    @Transactional(readOnly = true)
    public Page<MemberDto> getMembers(UUID classId, UUID currentUserId, Pageable pageable) {
        classSecurityService.requireMember(classId, currentUserId);

        return classMemberRepository.findAllByClassIdOrderByJoinedAtAsc(classId, pageable)
                .map(this::toMemberDto);
    }

    @Transactional
    public void removeMember(UUID classId, UUID memberUserId, UUID currentUserId) {
        classSecurityService.requireOwner(classId, currentUserId);

        ClassMemberEntity member = classMemberRepository.findByClassIdAndUserId(classId, memberUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in class: " + memberUserId));

        if (member.getRole() == Role.OWNER) {
            throw new ForbiddenException("Cannot remove OWNER from class");
        }

        classMemberRepository.delete(member);
    }

    @Transactional
    public MemberDto assignRole(UUID classId, UUID memberUserId, AssignRoleRequest request, UUID currentUserId) {
        classSecurityService.requireOwner(classId, currentUserId);

        ClassMemberEntity member = classMemberRepository.findByClassIdAndUserId(classId, memberUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in class: " + memberUserId));

        if (member.getRole() == Role.OWNER) {
            throw new ForbiddenException("Cannot change OWNER role");
        }
        if (request.role() == Role.OWNER) {
            throw new ForbiddenException("Cannot assign OWNER role");
        }

        member.setRole(request.role());
        ClassMemberEntity saved = classMemberRepository.save(member);

        return toMemberDto(saved);
    }

    private MemberDto toMemberDto(ClassMemberEntity member) {
        UserEntity user = userRepository.findById(member.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + member.getUserId()));
        return new MemberDto(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                member.getRole(),
                member.getJoinedAt(),
                user.getAvatarUrl()
        );
    }
}

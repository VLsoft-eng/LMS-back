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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final ClassMemberRepository classMemberRepository;
    private final UserRepository        userRepository;
    private final ClassSecurityService  classSecurityService;

    @Transactional(readOnly = true)
    public List<MemberDto> getMembers(UUID classId, UUID currentUserId) {
        classSecurityService.requireMember(classId, currentUserId);

        return classMemberRepository.findAllByClassIdOrderByJoinedAtAsc(classId).stream()
                .map(this::toMemberDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public MemberDto assignRole(UUID classId, UUID memberUserId, AssignRoleRequest request, UUID currentUserId) {
        classSecurityService.requireOwner(classId, currentUserId);

        ClassMemberEntity member = classMemberRepository.findByClassIdAndUserId(classId, memberUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in class: " + memberUserId));

        if (member.getRole() == Role.OWNER) {
            throw new ForbiddenException("Cannot change OWNER role");
        }

        Role newRole = Role.valueOf(request.role());
        member.setRole(newRole);
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
                member.getRole().name(),
                member.getJoinedAt()
        );
    }
}

package com.example.lms.service;

import com.example.lms.entity.ClassMemberEntity;
import com.example.lms.entity.Role;
import com.example.lms.exception.ForbiddenException;
import com.example.lms.exception.ResourceNotFoundException;
import com.example.lms.repository.ClassMemberRepository;
import com.example.lms.repository.ClassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClassSecurityService {

    private final ClassRepository       classRepository;
    private final ClassMemberRepository classMemberRepository;

    @Transactional(readOnly = true)
    public ClassMemberEntity requireMember(UUID classId, UUID userId) {
        classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + classId));
        return classMemberRepository.findByClassIdAndUserId(classId, userId)
                .orElseThrow(() -> new ForbiddenException("Not a member of class: " + classId));
    }

    @Transactional(readOnly = true)
    public ClassMemberEntity requireOwner(UUID classId, UUID userId) {
        ClassMemberEntity member = requireMember(classId, userId);
        if (member.getRole() != Role.OWNER) {
            throw new ForbiddenException("OWNER role required for class: " + classId);
        }
        return member;
    }

    @Transactional(readOnly = true)
    public ClassMemberEntity requireOwnerOrTeacher(UUID classId, UUID userId) {
        ClassMemberEntity member = requireMember(classId, userId);
        if (member.getRole() != Role.OWNER && member.getRole() != Role.TEACHER) {
            throw new ForbiddenException("OWNER or TEACHER role required for class: " + classId);
        }
        return member;
    }
}

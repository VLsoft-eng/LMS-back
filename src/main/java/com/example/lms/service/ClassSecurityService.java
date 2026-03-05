package com.example.lms.service;

import com.example.lms.entity.ClassMemberEntity;
import com.example.lms.repository.ClassMemberRepository;
import com.example.lms.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClassSecurityService {

    private final ClassMemberRepository classMemberRepository;

    public ClassMemberEntity requireMember(UUID classId, UUID userId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public ClassMemberEntity requireOwner(UUID classId, UUID userId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public ClassMemberEntity requireOwnerOrTeacher(UUID classId, UUID userId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}

package com.example.lms.service;

import com.example.lms.dto.AssignRoleRequest;
import com.example.lms.dto.MemberDto;
import com.example.lms.repository.ClassMemberRepository;
import com.example.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final ClassMemberRepository classMemberRepository;
    private final UserRepository        userRepository;
    private final ClassSecurityService  classSecurityService;

    @Transactional(readOnly = true)
    public List<MemberDto> getMembers(UUID classId, UUID currentUserId) {
        throw new UnsupportedOperationException("TDD: implement getMembers");
    }

    @Transactional
    public MemberDto assignRole(UUID classId, UUID memberUserId, AssignRoleRequest request, UUID currentUserId) {
        throw new UnsupportedOperationException("TDD: implement assignRole");
    }
}

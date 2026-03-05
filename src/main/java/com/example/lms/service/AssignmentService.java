package com.example.lms.service;

import com.example.lms.dto.AssignmentDetailDto;
import com.example.lms.dto.AssignmentDto;
import com.example.lms.dto.CreateAssignmentRequest;
import com.example.lms.entity.UserEntity;
import com.example.lms.repository.AssignmentRepository;
import com.example.lms.repository.SubmissionRepository;
import com.example.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final UserRepository       userRepository;
    private final ClassSecurityService classSecurityService;

    @Transactional(readOnly = true)
    public List<AssignmentDto> getAssignments(UUID classId, UUID currentUserId) {
        throw new UnsupportedOperationException("TDD: implement getAssignments");
    }

    @Transactional
    public AssignmentDto createAssignment(UUID classId, CreateAssignmentRequest request, UserEntity currentUser) {
        throw new UnsupportedOperationException("TDD: implement createAssignment");
    }

    @Transactional(readOnly = true)
    public AssignmentDetailDto getAssignment(UUID assignmentId, UUID currentUserId) {
        throw new UnsupportedOperationException("TDD: implement getAssignment");
    }
}

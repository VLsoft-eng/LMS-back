package com.example.lms.service;

import com.example.lms.dto.GradeRequest;
import com.example.lms.dto.SubmissionDto;
import com.example.lms.entity.UserEntity;
import com.example.lms.repository.AssignmentRepository;
import com.example.lms.repository.SubmissionRepository;
import com.example.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final ClassSecurityService classSecurityService;
    private final FileStorageServiceImpl fileStorageService;

    public SubmissionDto submit(UUID assignmentId, String answerText, MultipartFile file, UserEntity student) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public List<SubmissionDto> getSubmissions(UUID assignmentId, UUID currentUserId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public SubmissionDto getMySubmission(UUID assignmentId, UUID studentId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public SubmissionDto grade(UUID submissionId, GradeRequest request, UUID currentUserId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}

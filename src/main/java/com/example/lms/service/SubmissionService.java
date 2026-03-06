package com.example.lms.service;

import com.example.lms.dto.GradeRequest;
import com.example.lms.dto.SubmissionDto;
import com.example.lms.entity.*;
import com.example.lms.exception.ForbiddenException;
import com.example.lms.exception.ResourceNotFoundException;
import com.example.lms.repository.AssignmentRepository;
import com.example.lms.repository.SubmissionRepository;
import com.example.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
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

    @Transactional
    public SubmissionDto submit(UUID assignmentId, String answerText, MultipartFile file, UserEntity student) {
        AssignmentEntity assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentId));

        ClassMemberEntity member = classSecurityService.requireMember(assignment.getClassId(), student.getId());
        if (member.getRole() != Role.STUDENT) {
            throw new ForbiddenException("Only STUDENT can submit assignments");
        }

        String filePath = null;
        if (file != null && !file.isEmpty()) {
            filePath = fileStorageService.store(file);
        }

        SubmissionEntity submission = submissionRepository
                .findByAssignmentIdAndStudentId(assignmentId, student.getId())
                .map(existing -> {
                    existing.setAnswerText(answerText);
                    if (filePath != null) {
                        existing.setFilePath(filePath);
                    }
                    existing.setSubmittedAt(Instant.now());
                    existing.setGrade(null);
                    existing.setGradedAt(null);
                    return existing;
                })
                .orElseGet(() -> SubmissionEntity.builder()
                        .assignmentId(assignmentId)
                        .studentId(student.getId())
                        .answerText(answerText)
                        .filePath(filePath)
                        .build());

        submission = submissionRepository.save(submission);
        return toDto(submission);
    }

    @Transactional(readOnly = true)
    public List<SubmissionDto> getSubmissions(UUID assignmentId, UUID currentUserId) {
        AssignmentEntity assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentId));

        classSecurityService.requireOwnerOrTeacher(assignment.getClassId(), currentUserId);

        return submissionRepository.findAllByAssignmentId(assignmentId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public SubmissionDto getMySubmission(UUID assignmentId, UUID studentId) {
        AssignmentEntity assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentId));

        classSecurityService.requireMember(assignment.getClassId(), studentId);

        SubmissionEntity submission = submissionRepository
                .findByAssignmentIdAndStudentId(assignmentId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found"));

        return toDto(submission);
    }

    @Transactional
    public SubmissionDto grade(UUID submissionId, GradeRequest request, UUID currentUserId) {
        SubmissionEntity submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found: " + submissionId));

        AssignmentEntity assignment = assignmentRepository.findById(submission.getAssignmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + submission.getAssignmentId()));

        classSecurityService.requireOwnerOrTeacher(assignment.getClassId(), currentUserId);

        submission.setGrade(request.grade().shortValue());
        submission.setGradedAt(Instant.now());
        submission = submissionRepository.save(submission);

        return toDto(submission);
    }

    private SubmissionDto toDto(SubmissionEntity entity) {
        UserEntity student = userRepository.findById(entity.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + entity.getStudentId()));

        String fileUrl = entity.getFilePath() != null
                ? "/api/v1/files/" + entity.getFilePath()
                : null;

        return new SubmissionDto(
                entity.getId(),
                entity.getStudentId(),
                student.getFirstName() + " " + student.getLastName(),
                entity.getAnswerText(),
                fileUrl,
                entity.getGrade() != null ? entity.getGrade().intValue() : null,
                entity.getSubmittedAt()
        );
    }
}

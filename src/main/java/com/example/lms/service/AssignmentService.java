package com.example.lms.service;

import com.example.lms.dto.AssignmentDetailDto;
import com.example.lms.dto.AssignmentDto;
import com.example.lms.entity.AssignmentEntity;
import com.example.lms.entity.ClassMemberEntity;
import com.example.lms.entity.Role;
import com.example.lms.entity.SubmissionEntity;
import com.example.lms.entity.UserEntity;
import com.example.lms.exception.ResourceNotFoundException;
import com.example.lms.repository.AssignmentRepository;
import com.example.lms.repository.SubmissionRepository;
import com.example.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository  assignmentRepository;
    private final SubmissionRepository  submissionRepository;
    private final UserRepository        userRepository;
    private final ClassSecurityService  classSecurityService;
    private final FileStorageServiceImpl fileStorageService;

    @Transactional(readOnly = true)
    public Page<AssignmentDto> getAssignments(UUID classId, UUID currentUserId, Pageable pageable) {
        ClassMemberEntity member = classSecurityService.requireMember(classId, currentUserId);

        return assignmentRepository.findAllByClassIdOrderByCreatedAtDesc(classId, pageable)
                .map(a -> toAssignmentDto(a, member.getRole(), currentUserId));
    }

    @Transactional
    public AssignmentDto createAssignment(UUID classId, String title, String description,
                                          Instant deadline, List<MultipartFile> files,
                                          UserEntity currentUser) {
        classSecurityService.requireOwnerOrTeacher(classId, currentUser.getId());

        List<String> filePaths = new ArrayList<>();
        if (files != null) {
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    filePaths.add(fileStorageService.store(file));
                }
            }
        }

        AssignmentEntity entity = AssignmentEntity.builder()
                .classId(classId)
                .title(title)
                .description(description)
                .deadline(deadline)
                .createdBy(currentUser.getId())
                .filePaths(filePaths)
                .build();
        entity = assignmentRepository.save(entity);

        return toAssignmentDto(entity, null, null);
    }

    @Transactional(readOnly = true)
    public AssignmentDetailDto getAssignment(UUID assignmentId, UUID currentUserId) {
        AssignmentEntity assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentId));

        ClassMemberEntity member = classSecurityService.requireMember(assignment.getClassId(), currentUserId);

        UserEntity creator = userRepository.findById(assignment.getCreatedBy())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + assignment.getCreatedBy()));
        String createdByName = creator.getFirstName() + " " + creator.getLastName();

        String submissionStatus = null;
        Integer grade = null;
        if (member.getRole() == Role.STUDENT) {
            Optional<SubmissionEntity> submission = submissionRepository
                    .findByAssignmentIdAndStudentId(assignmentId, currentUserId);
            if (submission.isEmpty()) {
                submissionStatus = "NOT_SUBMITTED";
            } else if (submission.get().getGrade() != null) {
                submissionStatus = "GRADED";
                grade = submission.get().getGrade().intValue();
            } else {
                submissionStatus = "SUBMITTED";
            }
        }

        List<String> fileUrls = Objects.requireNonNullElse(assignment.getFilePaths(), List.<String>of()).stream()
                .map(p -> "/api/v1/files/" + p)
                .toList();

        return new AssignmentDetailDto(
                assignment.getId(),
                assignment.getClassId(),
                assignment.getTitle(),
                assignment.getDescription(),
                assignment.getCreatedBy(),
                createdByName,
                assignment.getType(),
                assignment.isTeamBased(),
                assignment.getDeadline(),
                assignment.getCreatedAt(),
                submissionStatus,
                grade,
                fileUrls
        );
    }

    private AssignmentDto toAssignmentDto(AssignmentEntity a, Role memberRole, UUID currentUserId) {
        String submissionStatus = null;
        Integer grade = null;
        if (memberRole == Role.STUDENT) {
            Optional<SubmissionEntity> submission = submissionRepository
                    .findByAssignmentIdAndStudentId(a.getId(), currentUserId);
            if (submission.isEmpty()) {
                submissionStatus = "NOT_SUBMITTED";
            } else if (submission.get().getGrade() != null) {
                submissionStatus = "GRADED";
                grade = submission.get().getGrade().intValue();
            } else {
                submissionStatus = "SUBMITTED";
            }
        }

        List<String> fileUrls = Objects.requireNonNullElse(a.getFilePaths(), List.<String>of()).stream()
                .map(p -> "/api/v1/files/" + p)
                .toList();

        return new AssignmentDto(
                a.getId(),
                a.getTitle(),
                a.getDescription(),
                a.getDeadline(),
                a.getType(),
                a.isTeamBased(),
                a.getCreatedAt(),
                submissionStatus,
                grade,
                fileUrls
        );
    }
}

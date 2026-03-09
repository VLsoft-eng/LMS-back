package com.example.lms.service;

import com.example.lms.dto.AssignmentDetailDto;
import com.example.lms.dto.AssignmentDto;
import com.example.lms.dto.CreateAssignmentRequest;
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

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final UserRepository       userRepository;
    private final ClassSecurityService classSecurityService;

    @Transactional(readOnly = true)
    public Page<AssignmentDto> getAssignments(UUID classId, UUID currentUserId, Pageable pageable) {
        ClassMemberEntity member = classSecurityService.requireMember(classId, currentUserId);

        return assignmentRepository.findAllByClassIdOrderByCreatedAtDesc(classId, pageable)
                .map(a -> toAssignmentDto(a, member.getRole(), currentUserId));
    }

    @Transactional
    public AssignmentDto createAssignment(UUID classId, CreateAssignmentRequest request, UserEntity currentUser) {
        classSecurityService.requireOwnerOrTeacher(classId, currentUser.getId());

        AssignmentEntity entity = AssignmentEntity.builder()
                .classId(classId)
                .title(request.title())
                .description(request.description())
                .deadline(request.deadline())
                .createdBy(currentUser.getId())
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

        return new AssignmentDetailDto(
                assignment.getId(),
                assignment.getClassId(),
                assignment.getTitle(),
                assignment.getDescription(),
                assignment.getCreatedBy(),
                createdByName,
                assignment.getDeadline(),
                assignment.getCreatedAt(),
                submissionStatus,
                grade
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

        return new AssignmentDto(
                a.getId(),
                a.getTitle(),
                a.getDescription(),
                a.getDeadline(),
                a.getCreatedAt(),
                submissionStatus,
                grade
        );
    }
}

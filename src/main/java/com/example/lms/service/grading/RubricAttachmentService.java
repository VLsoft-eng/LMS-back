package com.example.lms.service.grading;

import com.example.lms.dto.grading.AdhocRubricInput;
import com.example.lms.dto.grading.AttachRubricRequest;
import com.example.lms.dto.grading.RubricDto;
import com.example.lms.entity.AssignmentEntity;
import com.example.lms.entity.UserEntity;
import com.example.lms.entity.grading.CriterionTemplateEntity;
import com.example.lms.entity.grading.RubricEntity;
import com.example.lms.entity.grading.RubricTemplateEntity;
import com.example.lms.exception.ResourceNotFoundException;
import com.example.lms.exception.RubricConflictException;
import com.example.lms.exception.RubricInvariantViolation;
import com.example.lms.repository.AssessmentRepository;
import com.example.lms.repository.AssignmentRepository;
import com.example.lms.repository.RubricRepository;
import com.example.lms.repository.RubricTemplateRepository;
import com.example.lms.repository.SubmissionRepository;
import com.example.lms.repository.TeamGradeRepository;
import com.example.lms.service.ClassSecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * TICKET-BE-37: привязка/открепление рубрики к заданию.
 */
@Service
@RequiredArgsConstructor
public class RubricAttachmentService {

    private final AssignmentRepository assignmentRepository;
    private final RubricRepository rubricRepository;
    private final RubricTemplateRepository rubricTemplateRepository;
    private final AssessmentRepository assessmentRepository;
    private final SubmissionRepository submissionRepository;
    private final TeamGradeRepository teamGradeRepository;
    private final ClassSecurityService classSecurityService;
    private final ApplicationEventPublisher events;

    @Transactional
    public RubricDto attach(UUID assignmentId, AttachRubricRequest request, UserEntity currentUser) {
        AssignmentEntity assignment = requireAssignment(assignmentId);
        classSecurityService.requireOwnerOrTeacher(assignment.getClassId(), currentUser.getId());

        boolean fromTemplate = request.fromTemplateId() != null;
        boolean adhoc = request.adhoc() != null;
        if (fromTemplate == adhoc) {
            throw new RubricInvariantViolation("RUBRIC_BODY_MUTUALLY_EXCLUSIVE",
                    "Provide exactly one of fromTemplateId or adhoc");
        }

        if (rubricRepository.findByAssignmentId(assignmentId).isPresent()) {
            throw new RubricConflictException("RUBRIC_ALREADY_ATTACHED",
                    "Rubric already attached to this assignment");
        }
        if (submissionRepository.existsByAssignmentIdAndGradeIsNotNull(assignmentId)
                || teamGradeRepository.existsByAssignmentId(assignmentId)) {
            throw new RubricConflictException("ASSIGNMENT_HAS_GRADES",
                    "Assignment already has scalar grades — remove them before attaching a rubric");
        }

        RubricEntity rubric;
        if (fromTemplate) {
            RubricTemplateEntity template = rubricTemplateRepository.findById(request.fromTemplateId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Rubric template not found: " + request.fromTemplateId()));
            if (!template.getClassId().equals(assignment.getClassId())) {
                throw new RubricConflictException("RUBRIC_TEMPLATE_IN_USE",
                        "Template belongs to another class");
            }
            rubric = RubricEntity.snapshotFrom(template, assignmentId);
        } else {
            rubric = buildAdhocRubric(request.adhoc(), assignmentId);
        }

        rubric = rubricRepository.save(rubric);

        assignment.setRubricId(rubric.getId());
        assignmentRepository.save(assignment);

        events.publishEvent(new RubricAttachedEvent(assignmentId, rubric.getId()));
        return RubricMapper.toDto(rubric);
    }

    @Transactional(readOnly = true)
    public RubricDto getByAssignment(UUID assignmentId, UUID currentUserId) {
        AssignmentEntity assignment = requireAssignment(assignmentId);
        classSecurityService.requireMember(assignment.getClassId(), currentUserId);

        RubricEntity rubric = rubricRepository.findByAssignmentId(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rubric not attached to assignment: " + assignmentId));
        return RubricMapper.toDto(rubric);
    }

    @Transactional
    public void detach(UUID assignmentId, UserEntity currentUser) {
        AssignmentEntity assignment = requireAssignment(assignmentId);
        classSecurityService.requireOwnerOrTeacher(assignment.getClassId(), currentUser.getId());

        RubricEntity rubric = rubricRepository.findByAssignmentId(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rubric not attached to assignment: " + assignmentId));

        if (assessmentRepository.existsByRubricId(rubric.getId())) {
            throw new RubricConflictException("RUBRIC_HAS_ASSESSMENTS",
                    "Detach is forbidden — rubric has assessments");
        }

        assignment.setRubricId(null);
        assignmentRepository.save(assignment);
        rubricRepository.delete(rubric);
    }

    private AssignmentEntity requireAssignment(UUID assignmentId) {
        return assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentId));
    }

    private RubricEntity buildAdhocRubric(AdhocRubricInput input, UUID assignmentId) {
        var dummyTemplate = RubricTemplateEntity.builder()
                .name(input.name())
                .description(input.description())
                .totalMaxPoints(input.totalMaxPoints())
                .allowOvercap(input.allowOvercap())
                .criteria(new ArrayList<>())
                .build();
        List<CriterionTemplateEntity> templates = new ArrayList<>();
        for (var c : input.criteria()) {
            templates.add(RubricTemplateMapper.toEntity(c));
        }
        dummyTemplate.setCriteria(templates);
        dummyTemplate.validateInvariants();

        var rubric = RubricEntity.builder()
                .assignmentId(assignmentId)
                .sourceTemplateId(null)
                .name(input.name())
                .description(input.description())
                .totalMaxPoints(input.totalMaxPoints())
                .allowOvercap(input.allowOvercap())
                .criteria(new ArrayList<>())
                .build();
        for (CriterionTemplateEntity t : templates) {
            rubric.getCriteria().add(com.example.lms.entity.grading.CriterionEntity.builder()
                    .ordinal(t.getOrdinal())
                    .title(t.getTitle())
                    .description(t.getDescription())
                    .kind(t.getKind())
                    .role(t.getRole())
                    .maxPoints(t.getMaxPoints())
                    .maxCoefficient(t.getMaxCoefficient())
                    .scoreMin(t.getScoreMin())
                    .scoreMax(t.getScoreMax())
                    .build());
        }
        return rubric;
    }

    public record RubricAttachedEvent(UUID assignmentId, UUID rubricId) {}
}

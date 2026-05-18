package com.example.lms.service.grading;

import com.example.lms.dto.grading.AssessmentDto;
import com.example.lms.dto.grading.CreateAssessmentRequest;
import com.example.lms.dto.grading.CriterionScoreInput;
import com.example.lms.dto.grading.MyAssessmentCriterionDto;
import com.example.lms.dto.grading.MyAssessmentDto;
import com.example.lms.dto.grading.UpdateAssessmentRequest;
import com.example.lms.entity.AssignmentEntity;
import com.example.lms.entity.Role;
import com.example.lms.entity.SubmissionEntity;
import com.example.lms.entity.TeamGradeEntity;
import com.example.lms.entity.UserEntity;
import com.example.lms.entity.grading.AssessmentEntity;
import com.example.lms.entity.grading.CriterionEntity;
import com.example.lms.entity.grading.CriterionScoreEntity;
import com.example.lms.entity.grading.RubricEntity;
import com.example.lms.exception.ForbiddenException;
import com.example.lms.exception.ResourceNotFoundException;
import com.example.lms.exception.RubricConflictException;
import com.example.lms.exception.RubricInvariantViolation;
import com.example.lms.repository.AssessmentRepository;
import com.example.lms.repository.AssignmentRepository;
import com.example.lms.repository.ClassMemberRepository;
import com.example.lms.repository.RubricRepository;
import com.example.lms.repository.SubmissionRepository;
import com.example.lms.repository.TeamGradeRepository;
import com.example.lms.repository.TeamMemberRepository;
import com.example.lms.service.ClassSecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * TICKET-BE-38: бизнес-логика ассессментов (создание/обновление/удаление и student-facing).
 */
@Service
@RequiredArgsConstructor
public class AssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final RubricRepository rubricRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final TeamGradeRepository teamGradeRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ClassMemberRepository classMemberRepository;
    private final ClassSecurityService classSecurityService;
    private final RubricScoreCalculator calculator;
    private final ApplicationEventPublisher events;

    @Transactional
    public AssessmentDto create(UUID assignmentId, CreateAssessmentRequest request, UserEntity currentUser) {
        AssignmentEntity assignment = requireAssignment(assignmentId);
        classSecurityService.requireOwnerOrTeacher(assignment.getClassId(), currentUser.getId());

        if (assignment.getRubricId() == null) {
            throw new ResourceNotFoundException("Rubric not attached to assignment: " + assignmentId);
        }
        RubricEntity rubric = rubricRepository.findById(assignment.getRubricId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rubric not found: " + assignment.getRubricId()));

        boolean hasSubmission = request.submissionId() != null;
        boolean hasTeamGrade = request.teamGradeId() != null;
        if (hasSubmission == hasTeamGrade) {
            throw new RubricConflictException("ASSESSMENT_TARGET_MUTUALLY_EXCLUSIVE",
                    "Provide exactly one of submissionId or teamGradeId");
        }

        if (hasSubmission) {
            if (assessmentRepository.findBySubmissionId(request.submissionId()).isPresent()) {
                throw new RubricConflictException("ASSESSMENT_ALREADY_EXISTS",
                        "Assessment already exists for this submission");
            }
            SubmissionEntity submission = submissionRepository.findById(request.submissionId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Submission not found: " + request.submissionId()));
            if (!submission.getAssignmentId().equals(assignmentId)) {
                throw new RubricConflictException("ASSESSMENT_TARGET_MUTUALLY_EXCLUSIVE",
                        "Submission does not belong to this assignment");
            }
        } else {
            if (assessmentRepository.findByTeamGradeId(request.teamGradeId()).isPresent()) {
                throw new RubricConflictException("ASSESSMENT_ALREADY_EXISTS",
                        "Assessment already exists for this team grade");
            }
            TeamGradeEntity tg = teamGradeRepository.findById(request.teamGradeId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Team grade not found: " + request.teamGradeId()));
            if (!tg.getAssignmentId().equals(assignmentId)) {
                throw new RubricConflictException("ASSESSMENT_TARGET_MUTUALLY_EXCLUSIVE",
                        "Team grade does not belong to this assignment");
            }
        }

        List<CriterionScoreEntity> rawScores = buildRawScores(rubric, request.scores());
        var result = calculator.calculate(rubric, rawScores);

        AssessmentEntity assessment = AssessmentEntity.builder()
                .rubricId(rubric.getId())
                .assignmentId(assignmentId)
                .submissionId(request.submissionId())
                .teamGradeId(request.teamGradeId())
                .primarySum(result.primarySum())
                .bonusMultiplier(result.bonusMultiplier())
                .finalScore(result.finalScore())
                .finalScoreNormalized(result.finalScoreNormalized())
                .gradedBy(currentUser.getId())
                .scores(rawScores)
                .build();
        assessment = assessmentRepository.save(assessment);

        propagateTargetGrade(assessment);
        events.publishEvent(new AssessmentEvents.AssessmentCreated(
                assessment.getId(), assessment.getSubmissionId(), assessment.getTeamGradeId(),
                assessment.getFinalScore()));
        return AssessmentMapper.toDto(assessment);
    }

    @Transactional
    public AssessmentDto update(UUID assessmentId, UpdateAssessmentRequest request, UserEntity currentUser) {
        AssessmentEntity assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment not found: " + assessmentId));
        AssignmentEntity assignment = requireAssignment(assessment.getAssignmentId());
        classSecurityService.requireOwnerOrTeacher(assignment.getClassId(), currentUser.getId());

        UUID rubricRef = assessment.getRubricId();
        RubricEntity rubric = rubricRepository.findById(rubricRef)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rubric not found: " + rubricRef));

        java.math.BigDecimal oldFinalScore = assessment.getFinalScore();

        List<CriterionScoreEntity> rawScores = buildRawScores(rubric, request.scores());
        var result = calculator.calculate(rubric, rawScores);

        assessment.getScores().clear();
        assessment.getScores().addAll(rawScores);
        assessment.setPrimarySum(result.primarySum());
        assessment.setBonusMultiplier(result.bonusMultiplier());
        assessment.setFinalScore(result.finalScore());
        assessment.setFinalScoreNormalized(result.finalScoreNormalized());
        assessment.setGradedBy(currentUser.getId());
        assessment.setGradedAt(Instant.now());
        assessment = assessmentRepository.save(assessment);

        propagateTargetGrade(assessment);
        events.publishEvent(new AssessmentEvents.AssessmentUpdated(
                assessment.getId(), oldFinalScore, assessment.getFinalScore()));
        return AssessmentMapper.toDto(assessment);
    }

    @Transactional
    public void delete(UUID assessmentId, UserEntity currentUser) {
        AssessmentEntity assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment not found: " + assessmentId));
        AssignmentEntity assignment = requireAssignment(assessment.getAssignmentId());
        classSecurityService.requireOwnerOrTeacher(assignment.getClassId(), currentUser.getId());

        if (assessment.getSubmissionId() != null) {
            submissionRepository.findById(assessment.getSubmissionId()).ifPresent(s -> {
                s.setGrade(null);
                s.setGradedAt(null);
                submissionRepository.save(s);
            });
        }

        UUID submissionId = assessment.getSubmissionId();
        UUID teamGradeId = assessment.getTeamGradeId();
        assessmentRepository.delete(assessment);
        events.publishEvent(new AssessmentEvents.AssessmentDeleted(
                assessmentId, submissionId, teamGradeId));
    }

    @Transactional(readOnly = true)
    public AssessmentDto getById(UUID assessmentId, UserEntity currentUser) {
        AssessmentEntity assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment not found: " + assessmentId));
        AssignmentEntity assignment = requireAssignment(assessment.getAssignmentId());
        ensureViewAccess(assessment, assignment, currentUser);
        return AssessmentMapper.toDto(assessment);
    }

    @Transactional(readOnly = true)
    public AssessmentDto getBySubmission(UUID submissionId, UserEntity currentUser) {
        AssessmentEntity assessment = assessmentRepository.findBySubmissionId(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Assessment not found for submission: " + submissionId));
        AssignmentEntity assignment = requireAssignment(assessment.getAssignmentId());
        ensureViewAccess(assessment, assignment, currentUser);
        return AssessmentMapper.toDto(assessment);
    }

    @Transactional(readOnly = true)
    public AssessmentDto getByTeamGrade(UUID teamGradeId, UserEntity currentUser) {
        AssessmentEntity assessment = assessmentRepository.findByTeamGradeId(teamGradeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Assessment not found for team grade: " + teamGradeId));
        AssignmentEntity assignment = requireAssignment(assessment.getAssignmentId());
        ensureViewAccess(assessment, assignment, currentUser);
        return AssessmentMapper.toDto(assessment);
    }

    @Transactional(readOnly = true)
    public List<MyAssessmentDto> listMyAssessments(UUID studentId) {
        List<SubmissionEntity> mySubs = submissionRepository.findAllByStudentId(studentId);

        List<MyAssessmentDto> result = new ArrayList<>();
        for (SubmissionEntity s : mySubs) {
            assessmentRepository.findBySubmissionId(s.getId()).ifPresent(a ->
                    result.add(buildMyAssessment(a)));
        }
        return result;
    }

    private MyAssessmentDto buildMyAssessment(AssessmentEntity assessment) {
        AssignmentEntity assignment = requireAssignment(assessment.getAssignmentId());
        RubricEntity rubric = rubricRepository.findById(assessment.getRubricId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rubric not found: " + assessment.getRubricId()));

        Map<UUID, CriterionEntity> byId = new HashMap<>();
        for (CriterionEntity c : rubric.getCriteria()) byId.put(c.getId(), c);

        List<MyAssessmentCriterionDto> details = new ArrayList<>();
        for (CriterionScoreEntity s : assessment.getScores()) {
            CriterionEntity c = byId.get(s.getCriterionId());
            if (c == null) continue;
            Object value = switch (c.getKind()) {
                case BOOLEAN -> s.getBoolValue();
                case PERCENT -> s.getPercentValue();
                case SCORE -> s.getScoreValue();
            };
            details.add(new MyAssessmentCriterionDto(
                    c.getTitle(),
                    c.getKind(),
                    c.getRole(),
                    value,
                    c.getMaxPoints(),
                    c.getMaxCoefficient(),
                    c.getScoreMin(),
                    c.getScoreMax(),
                    s.getComputedPoints(),
                    s.getComment()
            ));
        }

        return new MyAssessmentDto(
                assignment.getId(),
                assignment.getTitle(),
                assessment.getId(),
                assessment.getFinalScore(),
                rubric.getTotalMaxPoints(),
                assessment.getFinalScoreNormalized(),
                details
        );
    }

    private void propagateTargetGrade(AssessmentEntity assessment) {
        Instant now = assessment.getGradedAt() != null ? assessment.getGradedAt() : Instant.now();
        if (assessment.getSubmissionId() != null) {
            submissionRepository.findById(assessment.getSubmissionId()).ifPresent(s -> {
                s.setGrade(assessment.getFinalScoreNormalized());
                s.setGradedAt(now);
                submissionRepository.save(s);
            });
        }
        if (assessment.getTeamGradeId() != null) {
            teamGradeRepository.findById(assessment.getTeamGradeId()).ifPresent(t -> {
                t.setGrade(assessment.getFinalScoreNormalized());
                t.setGradedAt(now);
                teamGradeRepository.save(t);
            });
        }
    }

    private List<CriterionScoreEntity> buildRawScores(RubricEntity rubric,
                                                       List<CriterionScoreInput> inputs) {
        if (inputs == null || inputs.size() != rubric.getCriteria().size()) {
            throw new RubricInvariantViolation("ASSESSMENT_SCORES_INCOMPLETE",
                    "All criteria must be scored");
        }
        List<CriterionScoreEntity> list = new ArrayList<>();
        for (CriterionScoreInput in : inputs) {
            list.add(CriterionScoreEntity.builder()
                    .criterionId(in.criterionId())
                    .boolValue(in.boolValue())
                    .percentValue(in.percentValue())
                    .scoreValue(in.scoreValue())
                    .computedPoints(java.math.BigDecimal.ZERO)
                    .comment(in.comment())
                    .build());
        }
        return list;
    }

    private AssignmentEntity requireAssignment(UUID assignmentId) {
        return assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentId));
    }

    private void ensureViewAccess(AssessmentEntity assessment, AssignmentEntity assignment, UserEntity user) {
        if (assessment.getSubmissionId() != null) {
            SubmissionEntity s = submissionRepository.findById(assessment.getSubmissionId()).orElse(null);
            if (s != null && s.getStudentId().equals(user.getId())) {
                return;
            }
        }
        if (assessment.getTeamGradeId() != null) {
            TeamGradeEntity tg = teamGradeRepository.findById(assessment.getTeamGradeId()).orElse(null);
            if (tg != null) {
                boolean inTeam = teamMemberRepository.findAllByUserId(user.getId()).stream()
                        .anyMatch(m -> m.getTeamId().equals(tg.getTeamId()));
                if (inTeam) return;
            }
        }
        var member = classMemberRepository.findByClassIdAndUserId(assignment.getClassId(), user.getId())
                .orElseThrow(() -> new ForbiddenException("Not a member of class: " + assignment.getClassId()));
        if (member.getRole() != Role.OWNER && member.getRole() != Role.TEACHER) {
            throw new ForbiddenException("Only OWNER/TEACHER or assessment owner can view this");
        }
    }
}

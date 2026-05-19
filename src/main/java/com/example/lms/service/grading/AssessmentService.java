package com.example.lms.service.grading;

import com.example.lms.dto.grading.AssessmentDto;
import com.example.lms.dto.grading.CreateAssessmentRequest;
import com.example.lms.dto.grading.CriterionScoreInput;
import com.example.lms.dto.grading.MyAssessmentCriterionDto;
import com.example.lms.dto.grading.MyAssessmentDto;
import com.example.lms.dto.grading.UpdateAssessmentRequest;
import com.example.lms.entity.AssignmentEntity;
import com.example.lms.entity.IndividualGradeAdjustmentEntity;
import com.example.lms.entity.Role;
import com.example.lms.entity.SubmissionEntity;
import com.example.lms.entity.TeamEntity;
import com.example.lms.entity.TeamGradeEntity;
import com.example.lms.entity.TeamMemberEntity;
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
import com.example.lms.repository.IndividualGradeAdjustmentRepository;
import com.example.lms.repository.RubricRepository;
import com.example.lms.repository.SubmissionRepository;
import com.example.lms.repository.TeamGradeRepository;
import com.example.lms.repository.TeamMemberRepository;
import com.example.lms.repository.TeamRepository;
import com.example.lms.service.ClassSecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * TICKET-BE-38 / BE-43: бизнес-логика ассессментов.
 *
 * Командное оценивание (BE-43): клиент присылает teamId; сервис атомарно
 * создаёт {@link TeamGradeEntity} и {@link IndividualGradeAdjustmentEntity}
 * для каждого члена команды, после чего создаёт Assessment с ссылкой на этот TeamGrade.
 * Параметр teamGradeId оставлен для обратной совместимости с legacy-flow.
 */
@Service
@RequiredArgsConstructor
public class AssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final RubricRepository rubricRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final TeamGradeRepository teamGradeRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final IndividualGradeAdjustmentRepository adjustmentRepository;
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

        TargetResolution target = resolveTarget(assignment, request, currentUser);

        List<CriterionScoreEntity> rawScores = buildRawScores(rubric, request.scores());
        var result = calculator.calculate(rubric, rawScores);

        AssessmentEntity assessment = AssessmentEntity.builder()
                .rubricId(rubric.getId())
                .assignmentId(assignmentId)
                .submissionId(target.submissionId())
                .teamGradeId(target.teamGradeId())
                .primarySum(result.primarySum())
                .bonusMultiplier(result.bonusMultiplier())
                .finalScore(result.finalScore())
                .finalScoreNormalized(result.finalScoreNormalized())
                .gradedBy(currentUser.getId())
                .scores(rawScores)
                .build();
        assessment = assessmentRepository.save(assessment);

        propagateTargetGrade(assessment, currentUser);
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

        BigDecimal oldFinalScore = assessment.getFinalScore();

        List<CriterionScoreEntity> rawScores = buildRawScores(rubric, request.scores());
        var result = calculator.calculate(rubric, rawScores);

        // orphanRemoval сам снесёт старые scores, но без flush Hibernate может попытаться
        // вставить новые до удаления старых, упираясь в UNIQUE (assessment_id, criterion_id).
        assessment.getScores().clear();
        assessmentRepository.saveAndFlush(assessment);
        assessment.getScores().addAll(rawScores);
        assessment.setPrimarySum(result.primarySum());
        assessment.setBonusMultiplier(result.bonusMultiplier());
        assessment.setFinalScore(result.finalScore());
        assessment.setFinalScoreNormalized(result.finalScoreNormalized());
        assessment.setGradedBy(currentUser.getId());
        assessment.setGradedAt(Instant.now());
        assessment = assessmentRepository.save(assessment);

        propagateTargetGrade(assessment, currentUser);
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

        UUID submissionId = assessment.getSubmissionId();
        UUID teamGradeId = assessment.getTeamGradeId();

        if (submissionId != null) {
            submissionRepository.findById(submissionId).ifPresent(s -> {
                s.setGrade(null);
                s.setGradedAt(null);
                submissionRepository.save(s);
            });
        }

        assessmentRepository.delete(assessment);

        if (teamGradeId != null) {
            // TeamGrade был создан как side-effect ассессмента (см. resolveTeamTarget):
            // удаляем его — CASCADE FK снесёт IndividualGradeAdjustment.
            // Если же TeamGrade пришёл через legacy teamGradeId — он жил независимо ещё до
            // нашего ассессмента; чтобы не сломать ту ветку, проверяем, есть ли другие связи.
            assessmentRepository.flush();
            teamGradeRepository.findById(teamGradeId).ifPresent(teamGradeRepository::delete);
        }

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

    // ────────────────────────────────────────────────────────────────────
    // private helpers
    // ────────────────────────────────────────────────────────────────────

    /**
     * Разбирает входной target (submissionId / teamId / teamGradeId), валидирует
     * принадлежность к assignment и при необходимости создаёт TeamGrade + adjustments.
     */
    private TargetResolution resolveTarget(AssignmentEntity assignment,
                                            CreateAssessmentRequest request,
                                            UserEntity currentUser) {
        UUID assignmentId = assignment.getId();
        int provided = 0;
        if (request.submissionId() != null) provided++;
        if (request.teamId() != null) provided++;
        if (request.teamGradeId() != null) provided++;
        if (provided != 1) {
            throw new RubricConflictException("ASSESSMENT_TARGET_MUTUALLY_EXCLUSIVE",
                    "Provide exactly one of submissionId, teamId or teamGradeId");
        }

        if (request.submissionId() != null) {
            return resolveSubmissionTarget(assignmentId, request.submissionId());
        }
        if (request.teamId() != null) {
            return resolveTeamTarget(assignment, request.teamId(), currentUser);
        }
        return resolveLegacyTeamGradeTarget(assignmentId, request.teamGradeId());
    }

    private TargetResolution resolveSubmissionTarget(UUID assignmentId, UUID submissionId) {
        if (assessmentRepository.findBySubmissionId(submissionId).isPresent()) {
            throw new RubricConflictException("ASSESSMENT_ALREADY_EXISTS",
                    "Assessment already exists for this submission");
        }
        SubmissionEntity submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Submission not found: " + submissionId));
        if (!submission.getAssignmentId().equals(assignmentId)) {
            throw new RubricConflictException("ASSESSMENT_TARGET_MUTUALLY_EXCLUSIVE",
                    "Submission does not belong to this assignment");
        }
        return new TargetResolution(submissionId, null);
    }

    /**
     * BE-43: командный target по teamId. Создаёт TeamGrade и индивидуальные корректировки,
     * если их ещё нет.
     */
    private TargetResolution resolveTeamTarget(AssignmentEntity assignment, UUID teamId, UserEntity currentUser) {
        TeamEntity team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found: " + teamId));

        if (!team.getClassId().equals(assignment.getClassId())) {
            throw new RubricConflictException("ASSESSMENT_TARGET_MUTUALLY_EXCLUSIVE",
                    "Team does not belong to this class");
        }
        if (team.getAssignmentId() != null && !team.getAssignmentId().equals(assignment.getId())) {
            throw new RubricConflictException("ASSESSMENT_TARGET_MUTUALLY_EXCLUSIVE",
                    "Team is bound to another assignment");
        }

        TeamGradeEntity teamGrade = teamGradeRepository
                .findByTeamIdAndAssignmentId(teamId, assignment.getId())
                .orElseGet(() -> createTeamGradeWithMembers(team, assignment.getId(), currentUser));

        if (assessmentRepository.findByTeamGradeId(teamGrade.getId()).isPresent()) {
            throw new RubricConflictException("ASSESSMENT_ALREADY_EXISTS",
                    "Assessment already exists for this team");
        }
        return new TargetResolution(null, teamGrade.getId());
    }

    private TargetResolution resolveLegacyTeamGradeTarget(UUID assignmentId, UUID teamGradeId) {
        if (assessmentRepository.findByTeamGradeId(teamGradeId).isPresent()) {
            throw new RubricConflictException("ASSESSMENT_ALREADY_EXISTS",
                    "Assessment already exists for this team grade");
        }
        TeamGradeEntity tg = teamGradeRepository.findById(teamGradeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Team grade not found: " + teamGradeId));
        if (!tg.getAssignmentId().equals(assignmentId)) {
            throw new RubricConflictException("ASSESSMENT_TARGET_MUTUALLY_EXCLUSIVE",
                    "Team grade does not belong to this assignment");
        }
        return new TargetResolution(null, teamGradeId);
    }

    private TeamGradeEntity createTeamGradeWithMembers(TeamEntity team, UUID assignmentId, UserEntity currentUser) {
        TeamGradeEntity grade = TeamGradeEntity.builder()
                .teamId(team.getId())
                .assignmentId(assignmentId)
                .grade((short) 0)                   // placeholder, переписывается в propagateTargetGrade
                .gradedBy(currentUser.getId())
                .build();
        grade = teamGradeRepository.save(grade);

        List<TeamMemberEntity> members = teamMemberRepository.findAllByTeamId(team.getId());
        for (TeamMemberEntity member : members) {
            adjustmentRepository.save(IndividualGradeAdjustmentEntity.builder()
                    .teamGradeId(grade.getId())
                    .studentId(member.getUserId())
                    .adjustment((short) 0)
                    .finalGrade((short) 0)          // placeholder, переписывается в propagateTargetGrade
                    .gradedBy(currentUser.getId())
                    .build());
        }
        return grade;
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
                assignment.getClassId(),
                assignment.getId(),
                assignment.getTitle(),
                assessment.getId(),
                assessment.getFinalScore(),
                rubric.getTotalMaxPoints(),
                assessment.getFinalScoreNormalized(),
                details
        );
    }

    private void propagateTargetGrade(AssessmentEntity assessment, UserEntity currentUser) {
        Instant now = assessment.getGradedAt() != null ? assessment.getGradedAt() : Instant.now();
        short normalized = assessment.getFinalScoreNormalized();

        if (assessment.getSubmissionId() != null) {
            submissionRepository.findById(assessment.getSubmissionId()).ifPresent(s -> {
                s.setGrade(normalized);
                s.setGradedAt(now);
                submissionRepository.save(s);
            });
        }
        if (assessment.getTeamGradeId() != null) {
            teamGradeRepository.findById(assessment.getTeamGradeId()).ifPresent(t -> {
                t.setGrade(normalized);
                t.setGradedAt(now);
                teamGradeRepository.save(t);

                // Пересчитываем finalGrade для всех членов команды: clamp(grade + adjustment).
                List<IndividualGradeAdjustmentEntity> adjustments =
                        adjustmentRepository.findAllByTeamGradeId(t.getId());
                for (IndividualGradeAdjustmentEntity adj : adjustments) {
                    adj.setFinalGrade(clamp(normalized + adj.getAdjustment()));
                    adj.setGradedAt(now);
                    adj.setGradedBy(currentUser.getId());
                    adjustmentRepository.save(adj);
                }
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
                    .computedPoints(BigDecimal.ZERO)
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

    private static short clamp(int value) {
        return (short) Math.max(0, Math.min(100, value));
    }

    private record TargetResolution(UUID submissionId, UUID teamGradeId) {}
}

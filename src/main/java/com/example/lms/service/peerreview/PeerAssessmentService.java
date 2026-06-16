package com.example.lms.service.peerreview;

import com.example.lms.dto.grading.CriterionScoreInput;
import com.example.lms.dto.peerreview.MyReceivedPeerAssessmentsDto;
import com.example.lms.dto.peerreview.PeerAssessmentDto;
import com.example.lms.dto.peerreview.PeerReviewAssignmentDto;
import com.example.lms.dto.peerreview.PeerReviewResultDto;
import com.example.lms.dto.peerreview.SubmitPeerAssessmentRequest;
import com.example.lms.entity.AssignmentEntity;
import com.example.lms.entity.SubmissionEntity;
import com.example.lms.entity.UserEntity;
import com.example.lms.entity.grading.CriterionEntity;
import com.example.lms.entity.grading.RubricEntity;
import com.example.lms.entity.peerreview.PeerAssessmentEntity;
import com.example.lms.entity.peerreview.PeerCriterionScoreEntity;
import com.example.lms.entity.peerreview.PeerReviewAssignmentEntity;
import com.example.lms.entity.peerreview.PeerReviewStatus;
import com.example.lms.exception.ForbiddenException;
import com.example.lms.exception.ResourceNotFoundException;
import com.example.lms.exception.RubricConflictException;
import com.example.lms.exception.RubricInvariantViolation;
import com.example.lms.repository.AssignmentRepository;
import com.example.lms.repository.PeerAssessmentRepository;
import com.example.lms.repository.PeerReviewAssignmentRepository;
import com.example.lms.repository.RubricRepository;
import com.example.lms.repository.SubmissionRepository;
import com.example.lms.repository.UserRepository;
import com.example.lms.service.ClassSecurityService;
import com.example.lms.service.grading.RubricScoreCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * TICKET #9183: выставление и просмотр peer-оценок студентами-рецензентами.
 *
 * <p>Безопасность:
 * <ul>
 *   <li>submitAssessment — только назначенный рецензент, статус PENDING
 *   <li>updateAssessment — только тот же рецензент, статус SUBMITTED
 *   <li>getMyReceivedAssessments — только автор submission (студент)
 *   <li>getAssignmentResults — только OWNER/TEACHER
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class PeerAssessmentService {

    private final PeerReviewAssignmentRepository praRepo;
    private final PeerAssessmentRepository assessmentRepo;
    private final RubricRepository rubricRepository;
    private final SubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final ClassSecurityService classSecurityService;
    private final RubricScoreCalculator calculator;

    // ─────────────────────────────────────────────────────────────────────────
    // Student: submit / update
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public PeerAssessmentDto submitAssessment(UUID praId,
                                               SubmitPeerAssessmentRequest request,
                                               UserEntity currentUser) {
        PeerReviewAssignmentEntity pra = requirePra(praId);

        if (!pra.getReviewerId().equals(currentUser.getId())) {
            throw new ForbiddenException("Only the assigned reviewer can submit this assessment");
        }
        if (pra.getStatus() != PeerReviewStatus.PENDING) {
            throw new RubricConflictException("PEER_ASSESSMENT_ALREADY_EXISTS",
                    "Assessment already submitted for this peer review assignment");
        }

        // Guard: self-review should never happen (DB CHECK + redundant app-layer check)
        SubmissionEntity submission = submissionRepository.findById(pra.getSubmissionId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Submission not found: " + pra.getSubmissionId()));
        if (submission.getStudentId().equals(currentUser.getId())) {
            throw new ForbiddenException("PEER_SELF_REVIEW_FORBIDDEN", "Cannot review your own submission");
        }

        RubricEntity rubric = loadRubricForAssignment(pra.getAssignmentId());
        List<PeerCriterionScoreEntity> rawScores = buildRawScores(rubric, request.scores());

        // Reuse existing RubricScoreCalculator — adapts to CriterionScoreEntity-like interface
        var result = calculateViaAdapter(rubric, rawScores);

        PeerAssessmentEntity assessment = PeerAssessmentEntity.builder()
                .peerReviewAssignmentId(praId)
                .rubricId(rubric.getId())
                .primarySum(result.primarySum())
                .bonusMultiplier(result.bonusMultiplier())
                .finalScore(result.finalScore())
                .finalScoreNormalized(result.finalScoreNormalized())
                .scores(rawScores)
                .build();

        assessment = assessmentRepo.save(assessment);

        // Mark PRA as submitted
        pra.setStatus(PeerReviewStatus.SUBMITTED);
        praRepo.save(pra);

        return PeerReviewMapper.toAssessmentDto(assessment);
    }

    @Transactional
    public PeerAssessmentDto updateAssessment(UUID praId,
                                               SubmitPeerAssessmentRequest request,
                                               UserEntity currentUser) {
        PeerReviewAssignmentEntity pra = requirePra(praId);

        if (!pra.getReviewerId().equals(currentUser.getId())) {
            throw new ForbiddenException("Only the assigned reviewer can update this assessment");
        }

        PeerAssessmentEntity assessment = assessmentRepo.findByPeerReviewAssignmentId(praId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No peer assessment found for assignment: " + praId));

        UUID rubricId = assessment.getRubricId();
        RubricEntity rubric = rubricRepository.findById(rubricId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rubric not found: " + rubricId));

        List<PeerCriterionScoreEntity> rawScores = buildRawScores(rubric, request.scores());
        var result = calculateViaAdapter(rubric, rawScores);

        // Clear old scores, save new
        assessment.getScores().clear();
        assessmentRepo.saveAndFlush(assessment);
        assessment.getScores().addAll(rawScores);
        assessment.setPrimarySum(result.primarySum());
        assessment.setBonusMultiplier(result.bonusMultiplier());
        assessment.setFinalScore(result.finalScore());
        assessment.setFinalScoreNormalized(result.finalScoreNormalized());

        assessment = assessmentRepo.save(assessment);
        return PeerReviewMapper.toAssessmentDto(assessment);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Student: query
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PeerReviewAssignmentDto> getMyAssignedReviews(UUID assignmentId,
                                                               UserEntity currentUser) {
        AssignmentEntity assignment = requireAssignment(assignmentId);
        classSecurityService.requireMember(assignment.getClassId(), currentUser.getId());

        List<PeerReviewAssignmentEntity> pras =
                praRepo.findAllByReviewerIdAndAssignmentId(currentUser.getId(), assignmentId);
        return buildAssignmentDtos(pras);
    }

    @Transactional(readOnly = true)
    public PeerReviewAssignmentDto getMyAssignment(UUID praId, UserEntity currentUser) {
        PeerReviewAssignmentEntity pra = requirePra(praId);

        // reviewer or owner/teacher
        if (!pra.getReviewerId().equals(currentUser.getId())) {
            AssignmentEntity assignment = requireAssignment(pra.getAssignmentId());
            classSecurityService.requireOwnerOrTeacher(assignment.getClassId(), currentUser.getId());
        }

        SubmissionEntity sub = submissionRepository.findById(pra.getSubmissionId()).orElse(null);
        UserEntity student = sub != null ? userRepository.findById(sub.getStudentId()).orElse(null) : null;
        return PeerReviewMapper.toAssignmentDto(pra, sub, student);
    }

    @Transactional(readOnly = true)
    public MyReceivedPeerAssessmentsDto getMyReceivedAssessments(UUID assignmentId,
                                                                   UserEntity currentUser) {
        AssignmentEntity assignment = requireAssignment(assignmentId);
        classSecurityService.requireMember(assignment.getClassId(), currentUser.getId());

        // Find current student's submission
        SubmissionEntity mySubmission = submissionRepository
                .findByAssignmentIdAndStudentId(assignmentId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No submission found for student in assignment: " + assignmentId));

        // All PRAs targeting my submission
        List<PeerReviewAssignmentEntity> pras = praRepo.findAllByAssignmentId(assignmentId).stream()
                .filter(p -> p.getSubmissionId().equals(mySubmission.getId())
                        && p.getStatus() == PeerReviewStatus.SUBMITTED)
                .toList();

        List<PeerAssessmentDto> dtos = new ArrayList<>();
        for (PeerReviewAssignmentEntity pra : pras) {
            assessmentRepo.findByPeerReviewAssignmentId(pra.getId())
                    .map(PeerReviewMapper::toAssessmentDto)
                    .ifPresent(dtos::add);
        }

        return new MyReceivedPeerAssessmentsDto(mySubmission.getId(), assignmentId, dtos);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Teacher: aggregated results
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PeerReviewResultDto> getAssignmentResults(UUID assignmentId,
                                                           UserEntity currentUser) {
        AssignmentEntity assignment = requireAssignment(assignmentId);
        classSecurityService.requireOwnerOrTeacher(assignment.getClassId(), currentUser.getId());

        List<PeerReviewAssignmentEntity> pras = praRepo.findAllByAssignmentId(assignmentId).stream()
                .filter(p -> p.getStatus() == PeerReviewStatus.SUBMITTED)
                .toList();

        // Group by submissionId
        Map<UUID, List<PeerAssessmentDto>> bySubmission = new HashMap<>();
        for (PeerReviewAssignmentEntity pra : pras) {
            assessmentRepo.findByPeerReviewAssignmentId(pra.getId()).ifPresent(a -> {
                bySubmission.computeIfAbsent(pra.getSubmissionId(), k -> new ArrayList<>())
                        .add(PeerReviewMapper.toAssessmentDto(a));
            });
        }

        List<PeerReviewResultDto> results = new ArrayList<>();
        for (Map.Entry<UUID, List<PeerAssessmentDto>> entry : bySubmission.entrySet()) {
            UUID submissionId = entry.getKey();
            List<PeerAssessmentDto> assessments = entry.getValue();

            SubmissionEntity sub = submissionRepository.findById(submissionId).orElse(null);
            UserEntity student = sub != null
                    ? userRepository.findById(sub.getStudentId()).orElse(null) : null;
            String studentName = student != null
                    ? student.getFirstName() + " " + student.getLastName() : "Unknown";

            double avg = assessments.stream()
                    .mapToInt(a -> a.finalScoreNormalized())
                    .average()
                    .orElse(0.0);

            results.add(new PeerReviewResultDto(
                    submissionId,
                    studentName,
                    avg,
                    assessments.size(),
                    assessments
            ));
        }
        return results;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private RubricEntity loadRubricForAssignment(UUID assignmentId) {
        AssignmentEntity assignment = requireAssignment(assignmentId);
        if (assignment.getRubricId() == null) {
            throw new ResourceNotFoundException("Assignment has no rubric: " + assignmentId);
        }
        return rubricRepository.findById(assignment.getRubricId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rubric not found: " + assignment.getRubricId()));
    }

    private List<PeerCriterionScoreEntity> buildRawScores(RubricEntity rubric,
                                                            List<CriterionScoreInput> inputs) {
        if (inputs == null || inputs.size() != rubric.getCriteria().size()) {
            throw new RubricInvariantViolation("ASSESSMENT_SCORES_INCOMPLETE",
                    "All criteria must be scored");
        }

        Map<UUID, CriterionEntity> byId = rubric.getCriteria().stream()
                .collect(Collectors.toMap(CriterionEntity::getId, c -> c));

        List<PeerCriterionScoreEntity> list = new ArrayList<>();
        for (CriterionScoreInput in : inputs) {
            CriterionEntity criterion = byId.get(in.criterionId());
            if (criterion == null) {
                throw new RubricInvariantViolation("ASSESSMENT_SCORE_CRITERION_NOT_FOUND",
                        "Criterion not found in rubric: " + in.criterionId());
            }
            validateScoreRange(criterion, in);

            list.add(PeerCriterionScoreEntity.builder()
                    .criterionId(in.criterionId())
                    .boolValue(in.boolValue())
                    .percentValue(in.percentValue())
                    .scoreValue(in.scoreValue())
                    .computedPoints(BigDecimal.ZERO)  // will be filled by calculateViaAdapter
                    .comment(in.comment())
                    .build());
        }
        return list;
    }

    /**
     * Adapts PeerCriterionScoreEntity list to RubricScoreCalculator by converting
     * to the expected CriterionScoreEntity format via the shared calculator.
     * Uses reflection-free approach: directly calls the calculator's algorithm via
     * a temporary CriterionScoreEntity adapter.
     */
    private RubricScoreCalculator.CalculationResult calculateViaAdapter(RubricEntity rubric,
                                                              List<PeerCriterionScoreEntity> scores) {
        // Build CriterionScoreEntity proxies to reuse RubricScoreCalculator
        List<com.example.lms.entity.grading.CriterionScoreEntity> adapted = scores.stream()
                .map(s -> com.example.lms.entity.grading.CriterionScoreEntity.builder()
                        .criterionId(s.getCriterionId())
                        .boolValue(s.getBoolValue())
                        .percentValue(s.getPercentValue())
                        .scoreValue(s.getScoreValue())
                        .computedPoints(BigDecimal.ZERO)
                        .comment(s.getComment())
                        .build())
                .collect(Collectors.toList());

        var result = calculator.calculate(rubric, adapted);

        // Write computed points back to peer score entities
        for (int i = 0; i < scores.size(); i++) {
            scores.get(i).setComputedPoints(adapted.get(i).getComputedPoints());
        }
        return result;
    }

    private void validateScoreRange(CriterionEntity c, CriterionScoreInput in) {
        switch (c.getKind()) {
            case SCORE -> {
                if (in.scoreValue() == null) {
                    throw new RubricInvariantViolation("ASSESSMENT_SCORE_TYPE_MISMATCH",
                            "score criterion requires scoreValue: " + c.getId());
                }
                if (c.getScoreMin() != null && in.scoreValue().compareTo(c.getScoreMin()) < 0) {
                    throw new RubricInvariantViolation("ASSESSMENT_SCORE_OUT_OF_RANGE",
                            "scoreValue below minimum for criterion: " + c.getId());
                }
                if (c.getScoreMax() != null && in.scoreValue().compareTo(c.getScoreMax()) > 0) {
                    throw new RubricInvariantViolation("ASSESSMENT_SCORE_OUT_OF_RANGE",
                            "scoreValue exceeds maximum for criterion: " + c.getId());
                }
            }
            case BOOLEAN -> {
                if (in.boolValue() == null) {
                    throw new RubricInvariantViolation("ASSESSMENT_SCORE_TYPE_MISMATCH",
                            "boolean criterion requires boolValue: " + c.getId());
                }
            }
            case PERCENT -> {
                if (in.percentValue() == null) {
                    throw new RubricInvariantViolation("ASSESSMENT_SCORE_TYPE_MISMATCH",
                            "percent criterion requires percentValue: " + c.getId());
                }
                if (in.percentValue().compareTo(BigDecimal.ZERO) < 0
                        || in.percentValue().compareTo(BigDecimal.valueOf(100)) > 0) {
                    throw new RubricInvariantViolation("ASSESSMENT_SCORE_OUT_OF_RANGE",
                            "percentValue must be 0–100 for criterion: " + c.getId());
                }
            }
        }
    }

    private List<PeerReviewAssignmentDto> buildAssignmentDtos(List<PeerReviewAssignmentEntity> pras) {
        return pras.stream()
                .map(pra -> {
                    SubmissionEntity sub = submissionRepository.findById(pra.getSubmissionId()).orElse(null);
                    UserEntity student = sub != null
                            ? userRepository.findById(sub.getStudentId()).orElse(null) : null;
                    return PeerReviewMapper.toAssignmentDto(pra, sub, student);
                })
                .toList();
    }

    private PeerReviewAssignmentEntity requirePra(UUID id) {
        return praRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Peer review assignment not found: " + id));
    }

    private AssignmentEntity requireAssignment(UUID id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + id));
    }
}

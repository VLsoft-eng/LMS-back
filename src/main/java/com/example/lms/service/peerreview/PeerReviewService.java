package com.example.lms.service.peerreview;

import com.example.lms.dto.peerreview.ConfigurePeerReviewRequest;
import com.example.lms.dto.peerreview.PeerReviewAssignmentDto;
import com.example.lms.dto.peerreview.PeerReviewSettingsDto;
import com.example.lms.entity.AssignmentEntity;
import com.example.lms.entity.SubmissionEntity;
import com.example.lms.entity.UserEntity;
import com.example.lms.entity.peerreview.PeerReviewAssignmentEntity;
import com.example.lms.entity.peerreview.PeerReviewSettingsEntity;
import com.example.lms.exception.ForbiddenException;
import com.example.lms.exception.RubricConflictException;
import com.example.lms.exception.ResourceNotFoundException;
import com.example.lms.repository.AssignmentRepository;
import com.example.lms.repository.PeerReviewAssignmentRepository;
import com.example.lms.repository.PeerReviewSettingsRepository;
import com.example.lms.repository.SubmissionRepository;
import com.example.lms.repository.UserRepository;
import com.example.lms.service.ClassSecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * TICKET #9182: конфигурация peer-review и распределение рецензентов.
 *
 * <p>Алгоритм distributeReviewers:
 * <ol>
 *   <li>Берём все submissions задания (только уникальные студенты).
 *   <li>Создаём смешанный список submissions-target для каждого студента-рецензента.
 *   <li>Round-robin: каждому студенту назначаем следующие reviewsPerStudent работ из
 *       перемешанного списка, пропуская свою собственную.
 *   <li>Идемпотентность: пропускаем уже существующие (reviewer, submission, assignment).
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class PeerReviewService {

    private final PeerReviewSettingsRepository settingsRepo;
    private final PeerReviewAssignmentRepository assignmentRepo;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final ClassSecurityService classSecurityService;

    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public PeerReviewSettingsDto configure(UUID assignmentId,
                                            ConfigurePeerReviewRequest request,
                                            UserEntity currentUser) {
        AssignmentEntity assignment = requireAssignment(assignmentId);
        classSecurityService.requireOwnerOrTeacher(assignment.getClassId(), currentUser.getId());

        if (assignment.getRubricId() == null) {
            throw new RubricConflictException("PEER_REVIEW_NO_RUBRIC",
                    "Assignment must have a rubric before enabling peer review");
        }

        PeerReviewSettingsEntity settings = settingsRepo.findByAssignmentId(assignmentId)
                .orElseGet(() -> PeerReviewSettingsEntity.builder()
                        .assignmentId(assignmentId)
                        .createdBy(currentUser.getId())
                        .build());

        settings.setReviewsPerStudent(request.reviewsPerStudent());
        settings.setEnabled(true);
        if (request.dueDate() != null) {
            settings.setDueDate(request.dueDate());
        }

        settings = settingsRepo.save(settings);
        return PeerReviewMapper.toSettingsDto(settings);
    }

    @Transactional(readOnly = true)
    public PeerReviewSettingsDto getSettings(UUID assignmentId, UserEntity currentUser) {
        AssignmentEntity assignment = requireAssignment(assignmentId);
        classSecurityService.requireMember(assignment.getClassId(), currentUser.getId());

        return settingsRepo.findByAssignmentId(assignmentId)
                .map(PeerReviewMapper::toSettingsDto)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Peer review not configured for assignment: " + assignmentId));
    }

    @Transactional
    public List<PeerReviewAssignmentDto> distributeReviewers(UUID assignmentId,
                                                              UserEntity currentUser) {
        AssignmentEntity assignment = requireAssignment(assignmentId);
        classSecurityService.requireOwnerOrTeacher(assignment.getClassId(), currentUser.getId());

        List<SubmissionEntity> submissions = submissionRepository.findAllByAssignmentId(assignmentId);
        if (submissions.size() < 2) {
            throw new RubricConflictException("PEER_REVIEW_NOT_ENOUGH_SUBMISSIONS",
                    "At least 2 submissions required to distribute peer reviewers");
        }

        PeerReviewSettingsEntity settings = settingsRepo.findByAssignmentId(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Peer review not configured for assignment: " + assignmentId));

        int reviewsPerStudent = Math.min(settings.getReviewsPerStudent(), submissions.size() - 1);

        // Build student → submission map
        Map<UUID, SubmissionEntity> submissionByStudent = new HashMap<>();
        for (SubmissionEntity s : submissions) {
            submissionByStudent.put(s.getStudentId(), s);
        }
        List<UUID> studentIds = new ArrayList<>(submissionByStudent.keySet());

        // Shuffle deterministically per-call for fair distribution
        Collections.shuffle(studentIds);

        // Pre-count existing assignments per reviewer to ensure idempotency
        Map<UUID, Long> existingCountByReviewer = new HashMap<>();
        for (UUID studentId : studentIds) {
            long count = assignmentRepo.findAllByReviewerIdAndAssignmentId(studentId, assignmentId).size();
            existingCountByReviewer.put(studentId, count);
        }

        List<PeerReviewAssignmentEntity> newAssignments = new ArrayList<>();

        for (int i = 0; i < studentIds.size(); i++) {
            UUID reviewerId = studentIds.get(i);
            long alreadyAssigned = existingCountByReviewer.getOrDefault(reviewerId, 0L);
            int needed = reviewsPerStudent - (int) alreadyAssigned;
            if (needed <= 0) continue; // already has enough assignments — skip

            int assigned = 0;
            int offset = 1;

            while (assigned < needed && offset < studentIds.size()) {
                int targetIdx = (i + offset) % studentIds.size();
                UUID targetStudentId = studentIds.get(targetIdx);
                SubmissionEntity targetSubmission = submissionByStudent.get(targetStudentId);

                if (!targetStudentId.equals(reviewerId)) {
                    boolean alreadyExists = assignmentRepo.existsByReviewerIdAndSubmissionIdAndAssignmentId(
                            reviewerId, targetSubmission.getId(), assignmentId);
                    if (!alreadyExists) {
                        newAssignments.add(PeerReviewAssignmentEntity.builder()
                                .assignmentId(assignmentId)
                                .reviewerId(reviewerId)
                                .submissionId(targetSubmission.getId())
                                .build());
                        assigned++;
                    }
                }
                offset++;
            }
        }

        if (!newAssignments.isEmpty()) {
            assignmentRepo.saveAll(newAssignments);
        }

        // Return all assignments (existing + new) for this assignment
        return buildAssignmentDtos(assignmentRepo.findAllByAssignmentId(assignmentId));
    }

    @Transactional(readOnly = true)
    public List<PeerReviewAssignmentDto> getAllAssignments(UUID assignmentId, UserEntity currentUser) {
        AssignmentEntity assignment = requireAssignment(assignmentId);
        classSecurityService.requireOwnerOrTeacher(assignment.getClassId(), currentUser.getId());
        return buildAssignmentDtos(assignmentRepo.findAllByAssignmentId(assignmentId));
    }

    // ─────────────────────────────────────────────────────────────────────────

    private List<PeerReviewAssignmentDto> buildAssignmentDtos(List<PeerReviewAssignmentEntity> entities) {
        // Pre-fetch all needed submissions + users
        Map<UUID, SubmissionEntity> submissionCache = new HashMap<>();
        Map<UUID, UserEntity> userCache = new HashMap<>();

        for (PeerReviewAssignmentEntity e : entities) {
            if (!submissionCache.containsKey(e.getSubmissionId())) {
                submissionRepository.findById(e.getSubmissionId())
                        .ifPresent(s -> submissionCache.put(s.getId(), s));
            }
        }
        for (SubmissionEntity s : submissionCache.values()) {
            userRepository.findById(s.getStudentId())
                    .ifPresent(u -> userCache.put(u.getId(), u));
        }

        return entities.stream()
                .map(e -> {
                    SubmissionEntity sub = submissionCache.get(e.getSubmissionId());
                    UserEntity student = sub != null ? userCache.get(sub.getStudentId()) : null;
                    return PeerReviewMapper.toAssignmentDto(e, sub, student);
                })
                .toList();
    }

    private AssignmentEntity requireAssignment(UUID id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + id));
    }
}

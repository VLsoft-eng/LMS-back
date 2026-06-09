package com.example.lms.repository;

import com.example.lms.entity.peerreview.PeerReviewAssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * TICKET #9180
 */
public interface PeerReviewAssignmentRepository extends JpaRepository<PeerReviewAssignmentEntity, UUID> {

    List<PeerReviewAssignmentEntity> findAllByReviewerIdAndAssignmentId(UUID reviewerId, UUID assignmentId);

    List<PeerReviewAssignmentEntity> findAllByAssignmentId(UUID assignmentId);

    Optional<PeerReviewAssignmentEntity> findByIdAndReviewerId(UUID id, UUID reviewerId);

    boolean existsByReviewerIdAndSubmissionIdAndAssignmentId(UUID reviewerId, UUID submissionId, UUID assignmentId);
}

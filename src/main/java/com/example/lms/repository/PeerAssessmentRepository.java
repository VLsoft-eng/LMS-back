package com.example.lms.repository;

import com.example.lms.entity.peerreview.PeerAssessmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * TICKET #9180
 */
public interface PeerAssessmentRepository extends JpaRepository<PeerAssessmentEntity, UUID> {

    Optional<PeerAssessmentEntity> findByPeerReviewAssignmentId(UUID peerReviewAssignmentId);
}

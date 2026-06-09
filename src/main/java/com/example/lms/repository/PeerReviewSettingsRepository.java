package com.example.lms.repository;

import com.example.lms.entity.peerreview.PeerReviewSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * TICKET #9180
 */
public interface PeerReviewSettingsRepository extends JpaRepository<PeerReviewSettingsEntity, UUID> {

    Optional<PeerReviewSettingsEntity> findByAssignmentId(UUID assignmentId);
}

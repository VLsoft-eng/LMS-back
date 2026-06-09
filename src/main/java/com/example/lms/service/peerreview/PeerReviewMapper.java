package com.example.lms.service.peerreview;

import com.example.lms.dto.peerreview.PeerAssessmentDto;
import com.example.lms.dto.peerreview.PeerCriterionScoreDto;
import com.example.lms.dto.peerreview.PeerReviewAssignmentDto;
import com.example.lms.dto.peerreview.PeerReviewSettingsDto;
import com.example.lms.entity.SubmissionEntity;
import com.example.lms.entity.UserEntity;
import com.example.lms.entity.peerreview.PeerAssessmentEntity;
import com.example.lms.entity.peerreview.PeerReviewAssignmentEntity;
import com.example.lms.entity.peerreview.PeerReviewSettingsEntity;

import java.util.List;

/**
 * TICKET #9182: маппер сущностей → DTO для peer-review.
 */
public final class PeerReviewMapper {

    private PeerReviewMapper() {}

    public static PeerReviewSettingsDto toSettingsDto(PeerReviewSettingsEntity e) {
        return new PeerReviewSettingsDto(
                e.getId(),
                e.getAssignmentId(),
                e.getReviewsPerStudent(),
                e.isEnabled(),
                e.getDueDate(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    public static PeerReviewAssignmentDto toAssignmentDto(PeerReviewAssignmentEntity e,
                                                           SubmissionEntity submission,
                                                           UserEntity student) {
        return new PeerReviewAssignmentDto(
                e.getId(),
                e.getAssignmentId(),
                e.getSubmissionId(),
                student != null ? student.getFirstName() + " " + student.getLastName() : "Unknown",
                student != null ? student.getAvatarUrl() : null,
                e.getStatus(),
                e.getCreatedAt()
        );
    }

    public static PeerAssessmentDto toAssessmentDto(PeerAssessmentEntity e) {
        List<PeerCriterionScoreDto> scores = e.getScores().stream()
                .map(s -> new PeerCriterionScoreDto(
                        s.getId(),
                        s.getCriterionId(),
                        s.getBoolValue(),
                        s.getPercentValue(),
                        s.getScoreValue(),
                        s.getComputedPoints(),
                        s.getComment()
                ))
                .toList();

        return new PeerAssessmentDto(
                e.getId(),
                e.getPeerReviewAssignmentId(),
                e.getRubricId(),
                e.getPrimarySum(),
                e.getBonusMultiplier(),
                e.getFinalScore(),
                e.getFinalScoreNormalized(),
                e.getSubmittedAt(),
                scores
        );
    }
}

package com.example.lms.entity.peerreview;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * TICKET #9180: оценка, выставленная студентом-рецензентом по критериям рубрики.
 * Параллельна {@link com.example.lms.entity.grading.AssessmentEntity} — изолированный контекст.
 */
@Entity
@Table(name = "peer_assessments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeerAssessmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "peer_review_assignment_id", nullable = false, unique = true)
    private UUID peerReviewAssignmentId;

    @Column(name = "rubric_id", nullable = false)
    private UUID rubricId;

    @Column(name = "primary_sum", nullable = false, precision = 10, scale = 2)
    private BigDecimal primarySum;

    @Column(name = "bonus_multiplier", nullable = false, precision = 6, scale = 4)
    private BigDecimal bonusMultiplier;

    @Column(name = "final_score", nullable = false, precision = 10, scale = 2)
    private BigDecimal finalScore;

    @Column(name = "final_score_normalized", nullable = false)
    private short finalScoreNormalized;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "peer_assessment_id", nullable = false)
    @Builder.Default
    private List<PeerCriterionScoreEntity> scores = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (submittedAt == null) submittedAt = Instant.now();
    }
}

package com.example.lms.entity.grading;

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
 * TICKET-BE-33: aggregate root оценивания по рубрике (один экземпляр на submission или teamGrade).
 */
@Entity
@Table(name = "assessments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "rubric_id", nullable = false)
    private UUID rubricId;

    @Column(name = "assignment_id", nullable = false)
    private UUID assignmentId;

    @Column(name = "submission_id")
    private UUID submissionId;

    @Column(name = "team_grade_id")
    private UUID teamGradeId;

    @Column(name = "primary_sum", nullable = false, precision = 10, scale = 2)
    private BigDecimal primarySum;

    @Column(name = "bonus_multiplier", nullable = false, precision = 6, scale = 4)
    private BigDecimal bonusMultiplier;

    @Column(name = "final_score", nullable = false, precision = 10, scale = 2)
    private BigDecimal finalScore;

    @Column(name = "final_score_normalized", nullable = false)
    private short finalScoreNormalized;

    @Column(name = "graded_by", nullable = false)
    private UUID gradedBy;

    @Column(name = "graded_at", nullable = false)
    private Instant gradedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "assessment_id", nullable = false)
    @Builder.Default
    private List<CriterionScoreEntity> scores = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (this.gradedAt == null) {
            this.gradedAt = Instant.now();
        }
    }
}

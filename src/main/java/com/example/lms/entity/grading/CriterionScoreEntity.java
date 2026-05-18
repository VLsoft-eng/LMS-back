package com.example.lms.entity.grading;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * TICKET-BE-33: значение оценки одного критерия внутри Assessment.
 */
@Entity
@Table(name = "criterion_scores")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CriterionScoreEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "assessment_id", nullable = false)
    private UUID assessmentId;

    @Column(name = "criterion_id", nullable = false)
    private UUID criterionId;

    @Column(name = "bool_value")
    private Boolean boolValue;

    @Column(name = "percent_value", precision = 5, scale = 2)
    private BigDecimal percentValue;

    @Column(name = "score_value", precision = 10, scale = 2)
    private BigDecimal scoreValue;

    @Column(name = "computed_points", nullable = false, precision = 10, scale = 4)
    private BigDecimal computedPoints;

    @Column(length = 500)
    private String comment;
}

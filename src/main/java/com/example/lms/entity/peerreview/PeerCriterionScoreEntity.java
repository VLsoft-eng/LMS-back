package com.example.lms.entity.peerreview;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * TICKET #9180: балл по одному критерию в рамках peer-оценки.
 */
@Entity
@Table(name = "peer_criterion_scores")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeerCriterionScoreEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "criterion_id", nullable = false)
    private UUID criterionId;

    @Column(name = "bool_value")
    private Boolean boolValue;

    @Column(name = "percent_value", precision = 5, scale = 2)
    private BigDecimal percentValue;

    @Column(name = "score_value", precision = 10, scale = 2)
    private BigDecimal scoreValue;

    @Column(name = "computed_points", nullable = false, precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal computedPoints = BigDecimal.ZERO;

    @Column(name = "comment", length = 500)
    private String comment;
}

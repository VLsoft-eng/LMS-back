package com.example.lms.entity.grading;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * TICKET-BE-32: критерий-снапшот внутри агрегата Rubric.
 */
@Entity
@Table(name = "criteria")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CriterionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "rubric_id", nullable = false, insertable = false, updatable = false)
    private UUID rubricId;

    @Column(nullable = false)
    private int ordinal;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CriterionKind kind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CriterionRole role;

    @Column(name = "max_points", precision = 10, scale = 2)
    private BigDecimal maxPoints;

    @Column(name = "max_coefficient", precision = 6, scale = 4)
    private BigDecimal maxCoefficient;

    @Column(name = "score_min", precision = 10, scale = 2)
    private BigDecimal scoreMin;

    @Column(name = "score_max", precision = 10, scale = 2)
    private BigDecimal scoreMax;
}

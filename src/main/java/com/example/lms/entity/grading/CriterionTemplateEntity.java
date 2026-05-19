package com.example.lms.entity.grading;

import com.example.lms.exception.RubricInvariantViolation;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * TICKET-BE-31: критерий-шаблон внутри агрегата RubricTemplate.
 */
@Entity
@Table(name = "criterion_templates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CriterionTemplateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "rubric_template_id", nullable = false, insertable = false, updatable = false)
    private UUID rubricTemplateId;

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

    /**
     * Проверка инвариантов критерия. Бросает {@link RubricInvariantViolation}
     * с кодом RUBRIC_CRITERION_INVALID при нарушении.
     */
    public void validateInvariants() {
        if (title == null || title.isBlank()) {
            throw new RubricInvariantViolation("RUBRIC_CRITERION_INVALID", "Title is required");
        }
        if (title.length() > 200) {
            throw new RubricInvariantViolation("RUBRIC_CRITERION_INVALID", "Title is too long");
        }
        if (description != null && description.length() > 2000) {
            throw new RubricInvariantViolation("RUBRIC_CRITERION_INVALID", "Description is too long");
        }
        if (kind == null || role == null) {
            throw new RubricInvariantViolation("RUBRIC_CRITERION_INVALID", "Kind and role are required");
        }
        if (ordinal < 0) {
            throw new RubricInvariantViolation("RUBRIC_CRITERION_INVALID", "Ordinal must be >= 0");
        }

        if (role == CriterionRole.PRIMARY) {
            if (maxPoints == null || maxPoints.signum() <= 0) {
                throw new RubricInvariantViolation("RUBRIC_CRITERION_INVALID",
                        "PRIMARY criterion requires maxPoints > 0");
            }
            if (maxCoefficient != null) {
                throw new RubricInvariantViolation("RUBRIC_CRITERION_INVALID",
                        "PRIMARY criterion must not have maxCoefficient");
            }
        } else {
            if (maxCoefficient == null) {
                throw new RubricInvariantViolation("RUBRIC_CRITERION_INVALID",
                        "BONUS criterion requires maxCoefficient");
            }
            BigDecimal min = new BigDecimal("1.0001");
            BigDecimal max = new BigDecimal("2.0000");
            if (maxCoefficient.compareTo(min) < 0 || maxCoefficient.compareTo(max) > 0) {
                throw new RubricInvariantViolation("RUBRIC_CRITERION_INVALID",
                        "BONUS maxCoefficient must be in [1.0001, 2.0000]");
            }
            if (maxPoints != null) {
                throw new RubricInvariantViolation("RUBRIC_CRITERION_INVALID",
                        "BONUS criterion must not have maxPoints");
            }
        }

        if (kind == CriterionKind.SCORE) {
            if (scoreMin == null || scoreMax == null) {
                throw new RubricInvariantViolation("RUBRIC_CRITERION_INVALID",
                        "SCORE criterion requires scoreMin and scoreMax");
            }
            if (scoreMax.compareTo(scoreMin) <= 0) {
                throw new RubricInvariantViolation("RUBRIC_CRITERION_INVALID",
                        "scoreMax must be greater than scoreMin");
            }
        } else {
            if (scoreMin != null || scoreMax != null) {
                throw new RubricInvariantViolation("RUBRIC_CRITERION_INVALID",
                        "scoreMin/scoreMax only allowed for SCORE kind");
            }
        }
    }
}

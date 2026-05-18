package com.example.lms.entity.grading;

import com.example.lms.exception.RubricInvariantViolation;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * TICKET-BE-31: aggregate root шаблона рубрики на уровне класса.
 */
@Entity
@Table(name = "rubric_templates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RubricTemplateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "class_id", nullable = false)
    private UUID classId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(name = "total_max_points", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalMaxPoints;

    @Column(name = "allow_overcap", nullable = false)
    @Builder.Default
    private boolean allowOvercap = false;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "rubric_template_id", nullable = false)
    @OrderBy("ordinal ASC")
    @Builder.Default
    private List<CriterionTemplateEntity> criteria = new ArrayList<>();

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * Полная проверка инвариантов агрегата (см. PRD §2.2).
     * 1) criteria.size() >= 1
     * 2) Σ maxPoints PRIMARY == totalMaxPoints
     * 3) Уникальность ordinal
     * 4) Валидность каждого критерия
     */
    public void validateInvariants() {
        if (name == null || name.isBlank()) {
            throw new RubricInvariantViolation("RUBRIC_CRITERION_INVALID", "Rubric name is required");
        }
        if (name.length() > 200) {
            throw new RubricInvariantViolation("RUBRIC_CRITERION_INVALID", "Rubric name is too long");
        }
        if (description != null && description.length() > 2000) {
            throw new RubricInvariantViolation("RUBRIC_CRITERION_INVALID", "Description is too long");
        }
        if (totalMaxPoints == null || totalMaxPoints.signum() <= 0) {
            throw new RubricInvariantViolation("RUBRIC_CRITERION_INVALID", "totalMaxPoints must be > 0");
        }
        if (totalMaxPoints.compareTo(new BigDecimal("1000")) > 0) {
            throw new RubricInvariantViolation("RUBRIC_CRITERION_INVALID", "totalMaxPoints must be <= 1000");
        }
        if (criteria == null || criteria.isEmpty()) {
            throw new RubricInvariantViolation("RUBRIC_CRITERION_INVALID", "At least one criterion is required");
        }
        if (criteria.size() > 50) {
            throw new RubricInvariantViolation("RUBRIC_CRITERION_INVALID", "Maximum 50 criteria per rubric");
        }

        Set<Integer> seenOrdinals = new HashSet<>();
        BigDecimal primarySum = BigDecimal.ZERO;
        for (CriterionTemplateEntity c : criteria) {
            c.validateInvariants();
            if (!seenOrdinals.add(c.getOrdinal())) {
                throw new RubricInvariantViolation("RUBRIC_CRITERION_INVALID",
                        "Duplicate criterion ordinal: " + c.getOrdinal());
            }
            if (c.getRole() == CriterionRole.PRIMARY) {
                primarySum = primarySum.add(c.getMaxPoints());
            }
        }

        if (primarySum.compareTo(totalMaxPoints) != 0) {
            throw new RubricInvariantViolation("RUBRIC_PRIMARY_SUM_MISMATCH",
                    "Sum of PRIMARY maxPoints (" + primarySum + ") must equal totalMaxPoints (" + totalMaxPoints + ")");
        }
    }
}

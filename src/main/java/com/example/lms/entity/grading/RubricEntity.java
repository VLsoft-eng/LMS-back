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
 * TICKET-BE-32: snapshot рубрики, привязанный к заданию. Иммутабелен после создания.
 */
@Entity
@Table(name = "rubrics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RubricEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "assignment_id", nullable = false, unique = true)
    private UUID assignmentId;

    @Column(name = "source_template_id")
    private UUID sourceTemplateId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(name = "total_max_points", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalMaxPoints;

    @Column(name = "allow_overcap", nullable = false)
    @Builder.Default
    private boolean allowOvercap = false;

    @Column(name = "frozen_at", nullable = false, updatable = false)
    private Instant frozenAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "rubric_id", nullable = false)
    @OrderBy("ordinal ASC")
    @Builder.Default
    private List<CriterionEntity> criteria = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (this.frozenAt == null) {
            this.frozenAt = Instant.now();
        }
    }

    /**
     * Создаёт неизменяемый снапшот рубрики из шаблона. UUID — новые, все поля копируются.
     */
    public static RubricEntity snapshotFrom(RubricTemplateEntity template, UUID assignmentId) {
        RubricEntity rubric = RubricEntity.builder()
                .assignmentId(assignmentId)
                .sourceTemplateId(template.getId())
                .name(template.getName())
                .description(template.getDescription())
                .totalMaxPoints(template.getTotalMaxPoints())
                .allowOvercap(template.isAllowOvercap())
                .criteria(new ArrayList<>())
                .build();

        for (CriterionTemplateEntity tpl : template.getCriteria()) {
            CriterionEntity c = CriterionEntity.builder()
                    .ordinal(tpl.getOrdinal())
                    .title(tpl.getTitle())
                    .description(tpl.getDescription())
                    .kind(tpl.getKind())
                    .role(tpl.getRole())
                    .maxPoints(tpl.getMaxPoints())
                    .maxCoefficient(tpl.getMaxCoefficient())
                    .scoreMin(tpl.getScoreMin())
                    .scoreMax(tpl.getScoreMax())
                    .build();
            rubric.getCriteria().add(c);
        }
        return rubric;
    }
}

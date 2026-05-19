package com.example.lms.entity.grading;

import com.example.lms.exception.RubricInvariantViolation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TICKET-BE-31: юнит-тесты на инварианты RubricTemplate и CriterionTemplate.
 */
class RubricTemplateInvariantsTest {

    private CriterionTemplateEntity primaryBoolean(int ordinal, String maxPoints) {
        return CriterionTemplateEntity.builder()
                .ordinal(ordinal)
                .title("c-" + ordinal)
                .kind(CriterionKind.BOOLEAN)
                .role(CriterionRole.PRIMARY)
                .maxPoints(new BigDecimal(maxPoints))
                .build();
    }

    private CriterionTemplateEntity primaryScore(int ordinal, String max, String min, String maxScore) {
        return CriterionTemplateEntity.builder()
                .ordinal(ordinal)
                .title("c-" + ordinal)
                .kind(CriterionKind.SCORE)
                .role(CriterionRole.PRIMARY)
                .maxPoints(new BigDecimal(max))
                .scoreMin(new BigDecimal(min))
                .scoreMax(new BigDecimal(maxScore))
                .build();
    }

    private CriterionTemplateEntity bonusBoolean(int ordinal, String coef) {
        return CriterionTemplateEntity.builder()
                .ordinal(ordinal)
                .title("bonus-" + ordinal)
                .kind(CriterionKind.BOOLEAN)
                .role(CriterionRole.BONUS)
                .maxCoefficient(new BigDecimal(coef))
                .build();
    }

    private RubricTemplateEntity newRubric(String total, List<CriterionTemplateEntity> criteria) {
        return RubricTemplateEntity.builder()
                .name("Rubric")
                .totalMaxPoints(new BigDecimal(total))
                .allowOvercap(false)
                .criteria(new ArrayList<>(criteria))
                .build();
    }

    @Test
    void valid_rubric_passes() {
        RubricTemplateEntity r = newRubric("10.00", List.of(
                primaryBoolean(0, "4.00"),
                primaryScore(1, "6.00", "0.00", "5.00")
        ));
        assertThatCode(r::validateInvariants).doesNotThrowAnyException();
    }

    @Test
    void empty_criteria_list_is_rejected() {
        RubricTemplateEntity r = newRubric("10.00", List.of());
        assertThatThrownBy(r::validateInvariants)
                .isInstanceOf(RubricInvariantViolation.class)
                .hasMessageContaining("At least one criterion");
    }

    @Test
    void primary_sum_mismatch_is_rejected_with_specific_code() {
        RubricTemplateEntity r = newRubric("10.00", List.of(
                primaryBoolean(0, "4.00"),
                primaryBoolean(1, "5.00")  // Σ = 9.00, не равно 10
        ));
        assertThatThrownBy(r::validateInvariants)
                .isInstanceOf(RubricInvariantViolation.class)
                .satisfies(e -> assertThat(((RubricInvariantViolation) e).getCode())
                        .isEqualTo("RUBRIC_PRIMARY_SUM_MISMATCH"));
    }

    @Test
    void duplicate_ordinals_are_rejected() {
        RubricTemplateEntity r = newRubric("10.00", List.of(
                primaryBoolean(0, "5.00"),
                primaryBoolean(0, "5.00")
        ));
        assertThatThrownBy(r::validateInvariants)
                .isInstanceOf(RubricInvariantViolation.class)
                .hasMessageContaining("Duplicate criterion ordinal");
    }

    @Test
    void primary_must_have_maxPoints() {
        CriterionTemplateEntity broken = CriterionTemplateEntity.builder()
                .ordinal(0)
                .title("c")
                .kind(CriterionKind.BOOLEAN)
                .role(CriterionRole.PRIMARY)
                .build();
        assertThatThrownBy(broken::validateInvariants)
                .isInstanceOf(RubricInvariantViolation.class)
                .hasMessageContaining("PRIMARY criterion requires maxPoints");
    }

    @Test
    void primary_must_not_have_maxCoefficient() {
        CriterionTemplateEntity broken = CriterionTemplateEntity.builder()
                .ordinal(0)
                .title("c")
                .kind(CriterionKind.BOOLEAN)
                .role(CriterionRole.PRIMARY)
                .maxPoints(new BigDecimal("1.00"))
                .maxCoefficient(new BigDecimal("1.5000"))
                .build();
        assertThatThrownBy(broken::validateInvariants)
                .isInstanceOf(RubricInvariantViolation.class)
                .hasMessageContaining("must not have maxCoefficient");
    }

    @Test
    void bonus_must_have_maxCoefficient() {
        CriterionTemplateEntity broken = CriterionTemplateEntity.builder()
                .ordinal(0)
                .title("b")
                .kind(CriterionKind.BOOLEAN)
                .role(CriterionRole.BONUS)
                .build();
        assertThatThrownBy(broken::validateInvariants)
                .isInstanceOf(RubricInvariantViolation.class)
                .hasMessageContaining("BONUS criterion requires maxCoefficient");
    }

    @Test
    void bonus_coefficient_must_be_in_range() {
        CriterionTemplateEntity tooLow = CriterionTemplateEntity.builder()
                .ordinal(0).title("b")
                .kind(CriterionKind.BOOLEAN).role(CriterionRole.BONUS)
                .maxCoefficient(new BigDecimal("1.0000"))
                .build();
        assertThatThrownBy(tooLow::validateInvariants)
                .isInstanceOf(RubricInvariantViolation.class)
                .hasMessageContaining("[1.0001, 2.0000]");

        CriterionTemplateEntity tooHigh = CriterionTemplateEntity.builder()
                .ordinal(0).title("b")
                .kind(CriterionKind.BOOLEAN).role(CriterionRole.BONUS)
                .maxCoefficient(new BigDecimal("2.0001"))
                .build();
        assertThatThrownBy(tooHigh::validateInvariants)
                .isInstanceOf(RubricInvariantViolation.class)
                .hasMessageContaining("[1.0001, 2.0000]");
    }

    @Test
    void score_kind_requires_min_and_max() {
        CriterionTemplateEntity broken = CriterionTemplateEntity.builder()
                .ordinal(0).title("c")
                .kind(CriterionKind.SCORE).role(CriterionRole.PRIMARY)
                .maxPoints(new BigDecimal("5.00"))
                .build();
        assertThatThrownBy(broken::validateInvariants)
                .isInstanceOf(RubricInvariantViolation.class)
                .hasMessageContaining("requires scoreMin and scoreMax");
    }

    @Test
    void score_max_must_be_greater_than_min() {
        CriterionTemplateEntity broken = CriterionTemplateEntity.builder()
                .ordinal(0).title("c")
                .kind(CriterionKind.SCORE).role(CriterionRole.PRIMARY)
                .maxPoints(new BigDecimal("5.00"))
                .scoreMin(new BigDecimal("5.00"))
                .scoreMax(new BigDecimal("5.00"))
                .build();
        assertThatThrownBy(broken::validateInvariants)
                .isInstanceOf(RubricInvariantViolation.class)
                .hasMessageContaining("scoreMax must be greater than scoreMin");
    }

    @Test
    void non_score_kind_must_not_have_scoreMin() {
        CriterionTemplateEntity broken = CriterionTemplateEntity.builder()
                .ordinal(0).title("c")
                .kind(CriterionKind.PERCENT).role(CriterionRole.PRIMARY)
                .maxPoints(new BigDecimal("3.00"))
                .scoreMin(new BigDecimal("0.00"))
                .build();
        assertThatThrownBy(broken::validateInvariants)
                .isInstanceOf(RubricInvariantViolation.class)
                .hasMessageContaining("only allowed for SCORE kind");
    }

    @Test
    void totalMaxPoints_must_be_positive() {
        RubricTemplateEntity r = newRubric("0.00", List.of(primaryBoolean(0, "0.01")));
        assertThatThrownBy(r::validateInvariants)
                .isInstanceOf(RubricInvariantViolation.class)
                .hasMessageContaining("totalMaxPoints must be > 0");
    }

    @Test
    void totalMaxPoints_caps_at_1000() {
        RubricTemplateEntity r = newRubric("1000.01", List.of(primaryBoolean(0, "1000.01")));
        assertThatThrownBy(r::validateInvariants)
                .isInstanceOf(RubricInvariantViolation.class)
                .hasMessageContaining("totalMaxPoints must be <= 1000");
    }

    @Test
    void bonus_does_not_count_towards_primary_sum() {
        RubricTemplateEntity r = newRubric("10.00", List.of(
                primaryBoolean(0, "10.00"),
                bonusBoolean(1, "1.1500")
        ));
        assertThatCode(r::validateInvariants).doesNotThrowAnyException();
    }

    @Test
    void empty_title_is_rejected() {
        RubricTemplateEntity r = RubricTemplateEntity.builder()
                .name("")
                .totalMaxPoints(new BigDecimal("5.00"))
                .criteria(new ArrayList<>(List.of(primaryBoolean(0, "5.00"))))
                .build();
        assertThatThrownBy(r::validateInvariants)
                .isInstanceOf(RubricInvariantViolation.class)
                .hasMessageContaining("name is required");
    }

    @Test
    void too_many_criteria_is_rejected() {
        List<CriterionTemplateEntity> tooMany = new ArrayList<>();
        for (int i = 0; i < 51; i++) {
            tooMany.add(primaryBoolean(i, "1.00"));
        }
        RubricTemplateEntity r = newRubric("51.00", tooMany);
        assertThatThrownBy(r::validateInvariants)
                .isInstanceOf(RubricInvariantViolation.class)
                .hasMessageContaining("Maximum 50 criteria");
    }
}

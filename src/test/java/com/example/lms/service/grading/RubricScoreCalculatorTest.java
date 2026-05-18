package com.example.lms.service.grading;

import com.example.lms.entity.grading.CriterionEntity;
import com.example.lms.entity.grading.CriterionKind;
import com.example.lms.entity.grading.CriterionRole;
import com.example.lms.entity.grading.CriterionScoreEntity;
import com.example.lms.entity.grading.RubricEntity;
import com.example.lms.exception.RubricInvariantViolation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TICKET-BE-33: 20+ кейсов для калькулятора (3 kind × 2 role + границы + округление + overcap).
 */
class RubricScoreCalculatorTest {

    private final RubricScoreCalculator calculator = new RubricScoreCalculator();

    private CriterionEntity criterion(CriterionKind kind, CriterionRole role, String maxPoints,
                                       String maxCoef, String scoreMin, String scoreMax) {
        return CriterionEntity.builder()
                .id(UUID.randomUUID())
                .ordinal(0)
                .title("c")
                .kind(kind)
                .role(role)
                .maxPoints(maxPoints == null ? null : new BigDecimal(maxPoints))
                .maxCoefficient(maxCoef == null ? null : new BigDecimal(maxCoef))
                .scoreMin(scoreMin == null ? null : new BigDecimal(scoreMin))
                .scoreMax(scoreMax == null ? null : new BigDecimal(scoreMax))
                .build();
    }

    private RubricEntity rubric(String total, boolean overcap, List<CriterionEntity> criteria) {
        return RubricEntity.builder()
                .id(UUID.randomUUID())
                .assignmentId(UUID.randomUUID())
                .name("R")
                .totalMaxPoints(new BigDecimal(total))
                .allowOvercap(overcap)
                .criteria(new ArrayList<>(criteria))
                .build();
    }

    private CriterionScoreEntity boolScore(UUID criterionId, boolean value) {
        return CriterionScoreEntity.builder().criterionId(criterionId).boolValue(value)
                .computedPoints(BigDecimal.ZERO).build();
    }

    private CriterionScoreEntity percentScore(UUID criterionId, String percent) {
        return CriterionScoreEntity.builder().criterionId(criterionId).percentValue(new BigDecimal(percent))
                .computedPoints(BigDecimal.ZERO).build();
    }

    private CriterionScoreEntity scoreScore(UUID criterionId, String value) {
        return CriterionScoreEntity.builder().criterionId(criterionId).scoreValue(new BigDecimal(value))
                .computedPoints(BigDecimal.ZERO).build();
    }

    // === PRIMARY ===

    @Test
    void primary_boolean_true_awards_full_points() {
        CriterionEntity c = criterion(CriterionKind.BOOLEAN, CriterionRole.PRIMARY, "5.00", null, null, null);
        RubricEntity r = rubric("5.00", false, List.of(c));
        var result = calculator.calculate(r, List.of(boolScore(c.getId(), true)));
        assertThat(result.primarySum()).isEqualByComparingTo("5.00");
        assertThat(result.finalScore()).isEqualByComparingTo("5.00");
        assertThat(result.finalScoreNormalized()).isEqualTo((short) 100);
    }

    @Test
    void primary_boolean_false_awards_zero() {
        CriterionEntity c = criterion(CriterionKind.BOOLEAN, CriterionRole.PRIMARY, "5.00", null, null, null);
        RubricEntity r = rubric("5.00", false, List.of(c));
        var result = calculator.calculate(r, List.of(boolScore(c.getId(), false)));
        assertThat(result.primarySum()).isEqualByComparingTo("0.00");
        assertThat(result.finalScoreNormalized()).isEqualTo((short) 0);
    }

    @Test
    void primary_percent_50_awards_half() {
        CriterionEntity c = criterion(CriterionKind.PERCENT, CriterionRole.PRIMARY, "10.00", null, null, null);
        RubricEntity r = rubric("10.00", false, List.of(c));
        var result = calculator.calculate(r, List.of(percentScore(c.getId(), "50.00")));
        assertThat(result.primarySum()).isEqualByComparingTo("5.00");
        assertThat(result.finalScoreNormalized()).isEqualTo((short) 50);
    }

    @Test
    void primary_percent_0() {
        CriterionEntity c = criterion(CriterionKind.PERCENT, CriterionRole.PRIMARY, "10.00", null, null, null);
        RubricEntity r = rubric("10.00", false, List.of(c));
        var result = calculator.calculate(r, List.of(percentScore(c.getId(), "0.00")));
        assertThat(result.primarySum()).isEqualByComparingTo("0.00");
    }

    @Test
    void primary_percent_100() {
        CriterionEntity c = criterion(CriterionKind.PERCENT, CriterionRole.PRIMARY, "10.00", null, null, null);
        RubricEntity r = rubric("10.00", false, List.of(c));
        var result = calculator.calculate(r, List.of(percentScore(c.getId(), "100.00")));
        assertThat(result.primarySum()).isEqualByComparingTo("10.00");
        assertThat(result.finalScoreNormalized()).isEqualTo((short) 100);
    }

    @Test
    void primary_score_at_min_awards_zero() {
        CriterionEntity c = criterion(CriterionKind.SCORE, CriterionRole.PRIMARY, "5.00", null, "0.00", "5.00");
        RubricEntity r = rubric("5.00", false, List.of(c));
        var result = calculator.calculate(r, List.of(scoreScore(c.getId(), "0.00")));
        assertThat(result.primarySum()).isEqualByComparingTo("0.00");
    }

    @Test
    void primary_score_at_max_awards_full() {
        CriterionEntity c = criterion(CriterionKind.SCORE, CriterionRole.PRIMARY, "5.00", null, "0.00", "5.00");
        RubricEntity r = rubric("5.00", false, List.of(c));
        var result = calculator.calculate(r, List.of(scoreScore(c.getId(), "5.00")));
        assertThat(result.primarySum()).isEqualByComparingTo("5.00");
    }

    @Test
    void primary_score_mid_with_non_zero_min() {
        // диапазон [2..6], значение 4 → 50%
        CriterionEntity c = criterion(CriterionKind.SCORE, CriterionRole.PRIMARY, "4.00", null, "2.00", "6.00");
        RubricEntity r = rubric("4.00", false, List.of(c));
        var result = calculator.calculate(r, List.of(scoreScore(c.getId(), "4.00")));
        assertThat(result.primarySum()).isEqualByComparingTo("2.00");
    }

    // === BONUS ===

    @Test
    void bonus_boolean_true_applies_full_multiplier() {
        CriterionEntity primary = criterion(CriterionKind.BOOLEAN, CriterionRole.PRIMARY, "10.00", null, null, null);
        CriterionEntity bonus = criterion(CriterionKind.BOOLEAN, CriterionRole.BONUS, null, "1.1500", null, null);
        RubricEntity r = rubric("10.00", true, List.of(primary, bonus));
        var result = calculator.calculate(r, List.of(
                boolScore(primary.getId(), true),
                boolScore(bonus.getId(), true)
        ));
        assertThat(result.bonusMultiplier()).isEqualByComparingTo("1.1500");
        assertThat(result.finalScore()).isEqualByComparingTo("11.50");
    }

    @Test
    void bonus_boolean_false_gives_multiplier_one() {
        CriterionEntity primary = criterion(CriterionKind.BOOLEAN, CriterionRole.PRIMARY, "10.00", null, null, null);
        CriterionEntity bonus = criterion(CriterionKind.BOOLEAN, CriterionRole.BONUS, null, "1.2000", null, null);
        RubricEntity r = rubric("10.00", true, List.of(primary, bonus));
        var result = calculator.calculate(r, List.of(
                boolScore(primary.getId(), true),
                boolScore(bonus.getId(), false)
        ));
        assertThat(result.bonusMultiplier()).isEqualByComparingTo("1.0000");
        assertThat(result.finalScore()).isEqualByComparingTo("10.00");
    }

    @Test
    void bonus_percent_partial_applies_proportionally() {
        // delta = 0.2, percent = 50 → contribution = 0.1
        CriterionEntity primary = criterion(CriterionKind.BOOLEAN, CriterionRole.PRIMARY, "10.00", null, null, null);
        CriterionEntity bonus = criterion(CriterionKind.PERCENT, CriterionRole.BONUS, null, "1.2000", null, null);
        RubricEntity r = rubric("10.00", true, List.of(primary, bonus));
        var result = calculator.calculate(r, List.of(
                boolScore(primary.getId(), true),
                percentScore(bonus.getId(), "50.00")
        ));
        assertThat(result.bonusMultiplier()).isEqualByComparingTo("1.1000");
    }

    @Test
    void bonus_score_at_max_applies_full() {
        // delta = 0.2, score = max → contribution = 0.2
        CriterionEntity primary = criterion(CriterionKind.BOOLEAN, CriterionRole.PRIMARY, "10.00", null, null, null);
        CriterionEntity bonus = criterion(CriterionKind.SCORE, CriterionRole.BONUS, null, "1.2000", "0.00", "5.00");
        RubricEntity r = rubric("10.00", true, List.of(primary, bonus));
        var result = calculator.calculate(r, List.of(
                boolScore(primary.getId(), true),
                scoreScore(bonus.getId(), "5.00")
        ));
        assertThat(result.bonusMultiplier()).isEqualByComparingTo("1.2000");
    }

    // === Overcap ===

    @Test
    void overcap_false_clamps_finalScore_to_totalMax() {
        CriterionEntity primary = criterion(CriterionKind.BOOLEAN, CriterionRole.PRIMARY, "10.00", null, null, null);
        CriterionEntity bonus = criterion(CriterionKind.BOOLEAN, CriterionRole.BONUS, null, "1.5000", null, null);
        RubricEntity r = rubric("10.00", false, List.of(primary, bonus));
        var result = calculator.calculate(r, List.of(
                boolScore(primary.getId(), true),
                boolScore(bonus.getId(), true)
        ));
        // raw = 15, clamp to 10
        assertThat(result.finalScore()).isEqualByComparingTo("10.00");
        assertThat(result.finalScoreNormalized()).isEqualTo((short) 100);
    }

    @Test
    void overcap_true_preserves_excess() {
        CriterionEntity primary = criterion(CriterionKind.BOOLEAN, CriterionRole.PRIMARY, "10.00", null, null, null);
        CriterionEntity bonus = criterion(CriterionKind.BOOLEAN, CriterionRole.BONUS, null, "1.5000", null, null);
        RubricEntity r = rubric("10.00", true, List.of(primary, bonus));
        var result = calculator.calculate(r, List.of(
                boolScore(primary.getId(), true),
                boolScore(bonus.getId(), true)
        ));
        // raw = 15
        assertThat(result.finalScore()).isEqualByComparingTo("15.00");
        // нормализация всё равно clamp до 100
        assertThat(result.finalScoreNormalized()).isEqualTo((short) 100);
    }

    @Test
    void primary_sum_cap_before_bonus_multiplier() {
        // Σ primary без cap = 12, capped до 10. Bonus 1.1 → finalScore = 11
        CriterionEntity c1 = criterion(CriterionKind.BOOLEAN, CriterionRole.PRIMARY, "7.00", null, null, null);
        CriterionEntity c2 = criterion(CriterionKind.BOOLEAN, CriterionRole.PRIMARY, "5.00", null, null, null);
        CriterionEntity bonus = criterion(CriterionKind.BOOLEAN, CriterionRole.BONUS, null, "1.1000", null, null);
        RubricEntity r = rubric("10.00", true, List.of(c1, c2, bonus));
        var result = calculator.calculate(r, List.of(
                boolScore(c1.getId(), true),
                boolScore(c2.getId(), true),
                boolScore(bonus.getId(), true)
        ));
        // primarySum = 12, capped = 10, mult = 1.1, final = 11
        assertThat(result.primarySum()).isEqualByComparingTo("12.00");
        assertThat(result.finalScore()).isEqualByComparingTo("11.00");
    }

    // === Округление ===

    @Test
    void rounding_half_up_for_points() {
        // 3.00 × 1/3 = 1.0000  → но 5 × (1/3) = 1.6666... → HALF_UP до 1.67
        CriterionEntity c = criterion(CriterionKind.PERCENT, CriterionRole.PRIMARY, "5.00", null, null, null);
        RubricEntity r = rubric("5.00", false, List.of(c));
        var result = calculator.calculate(r, List.of(percentScore(c.getId(), "33.33")));
        // 5 * 0.3333 = 1.6665 → HALF_UP до 1.67
        assertThat(result.primarySum()).isEqualByComparingTo("1.67");
    }

    @Test
    void rounding_coefficient_to_4_scale() {
        CriterionEntity primary = criterion(CriterionKind.BOOLEAN, CriterionRole.PRIMARY, "10.00", null, null, null);
        CriterionEntity bonus = criterion(CriterionKind.PERCENT, CriterionRole.BONUS, null, "1.3333", null, null);
        RubricEntity r = rubric("10.00", true, List.of(primary, bonus));
        var result = calculator.calculate(r, List.of(
                boolScore(primary.getId(), true),
                percentScore(bonus.getId(), "100.00")
        ));
        assertThat(result.bonusMultiplier()).isEqualByComparingTo("1.3333");
    }

    // === PRD пример ===

    @Test
    void prd_example_lab3_full_score() {
        CriterionEntity c1 = criterion(CriterionKind.BOOLEAN, CriterionRole.PRIMARY, "2.00", null, null, null);
        CriterionEntity c2 = criterion(CriterionKind.PERCENT, CriterionRole.PRIMARY, "3.00", null, null, null);
        CriterionEntity c3 = criterion(CriterionKind.SCORE, CriterionRole.PRIMARY, "5.00", null, "0.00", "5.00");
        CriterionEntity bonus = criterion(CriterionKind.BOOLEAN, CriterionRole.BONUS, null, "1.1500", null, null);
        RubricEntity r = rubric("10.00", false, List.of(c1, c2, c3, bonus));
        var result = calculator.calculate(r, List.of(
                boolScore(c1.getId(), true),
                percentScore(c2.getId(), "80.00"),
                scoreScore(c3.getId(), "4.50"),
                boolScore(bonus.getId(), true)
        ));
        // primarySum = 2 + 2.40 + 4.50 = 8.90
        // raw final = 8.90 * 1.15 = 10.235 → clamp до 10.00
        assertThat(result.primarySum()).isEqualByComparingTo("8.90");
        assertThat(result.bonusMultiplier()).isEqualByComparingTo("1.1500");
        assertThat(result.finalScore()).isEqualByComparingTo("10.00");
        assertThat(result.finalScoreNormalized()).isEqualTo((short) 100);
    }

    @Test
    void prd_example_lab3_with_overcap() {
        CriterionEntity c1 = criterion(CriterionKind.BOOLEAN, CriterionRole.PRIMARY, "2.00", null, null, null);
        CriterionEntity c2 = criterion(CriterionKind.PERCENT, CriterionRole.PRIMARY, "3.00", null, null, null);
        CriterionEntity c3 = criterion(CriterionKind.SCORE, CriterionRole.PRIMARY, "5.00", null, "0.00", "5.00");
        CriterionEntity bonus = criterion(CriterionKind.BOOLEAN, CriterionRole.BONUS, null, "1.1500", null, null);
        RubricEntity r = rubric("10.00", true, List.of(c1, c2, c3, bonus));
        var result = calculator.calculate(r, List.of(
                boolScore(c1.getId(), true),
                percentScore(c2.getId(), "80.00"),
                scoreScore(c3.getId(), "4.50"),
                boolScore(bonus.getId(), true)
        ));
        // Overcap = true → finalScore = 10.235 → HALF_UP до 10.24
        assertThat(result.finalScore()).isEqualByComparingTo("10.24");
    }

    // === Валидация форм значений ===

    @Test
    void boolean_score_without_value_is_rejected() {
        CriterionEntity c = criterion(CriterionKind.BOOLEAN, CriterionRole.PRIMARY, "5.00", null, null, null);
        RubricEntity r = rubric("5.00", false, List.of(c));
        CriterionScoreEntity s = CriterionScoreEntity.builder().criterionId(c.getId())
                .computedPoints(BigDecimal.ZERO).build();
        assertThatThrownBy(() -> calculator.calculate(r, List.of(s)))
                .isInstanceOf(RubricInvariantViolation.class)
                .satisfies(e -> assertThat(((RubricInvariantViolation) e).getCode())
                        .isEqualTo("ASSESSMENT_SCORE_TYPE_MISMATCH"));
    }

    @Test
    void percent_with_bool_value_is_rejected() {
        CriterionEntity c = criterion(CriterionKind.PERCENT, CriterionRole.PRIMARY, "5.00", null, null, null);
        RubricEntity r = rubric("5.00", false, List.of(c));
        CriterionScoreEntity s = CriterionScoreEntity.builder().criterionId(c.getId())
                .boolValue(true)
                .percentValue(new BigDecimal("50.00"))
                .computedPoints(BigDecimal.ZERO).build();
        assertThatThrownBy(() -> calculator.calculate(r, List.of(s)))
                .isInstanceOf(RubricInvariantViolation.class)
                .hasMessageContaining("PERCENT criterion must not have bool");
    }

    @Test
    void percent_out_of_range_high_is_rejected() {
        CriterionEntity c = criterion(CriterionKind.PERCENT, CriterionRole.PRIMARY, "5.00", null, null, null);
        RubricEntity r = rubric("5.00", false, List.of(c));
        assertThatThrownBy(() -> calculator.calculate(r, List.of(percentScore(c.getId(), "101.00"))))
                .isInstanceOf(RubricInvariantViolation.class)
                .satisfies(e -> assertThat(((RubricInvariantViolation) e).getCode())
                        .isEqualTo("ASSESSMENT_SCORE_OUT_OF_RANGE"));
    }

    @Test
    void percent_out_of_range_low_is_rejected() {
        CriterionEntity c = criterion(CriterionKind.PERCENT, CriterionRole.PRIMARY, "5.00", null, null, null);
        RubricEntity r = rubric("5.00", false, List.of(c));
        assertThatThrownBy(() -> calculator.calculate(r, List.of(percentScore(c.getId(), "-0.01"))))
                .isInstanceOf(RubricInvariantViolation.class)
                .hasMessageContaining("must be in [0, 100]");
    }

    @Test
    void score_out_of_range_is_rejected() {
        CriterionEntity c = criterion(CriterionKind.SCORE, CriterionRole.PRIMARY, "5.00", null, "0.00", "5.00");
        RubricEntity r = rubric("5.00", false, List.of(c));
        assertThatThrownBy(() -> calculator.calculate(r, List.of(scoreScore(c.getId(), "5.01"))))
                .isInstanceOf(RubricInvariantViolation.class)
                .satisfies(e -> assertThat(((RubricInvariantViolation) e).getCode())
                        .isEqualTo("ASSESSMENT_SCORE_OUT_OF_RANGE"));
    }

    @Test
    void missing_score_for_criterion_is_rejected() {
        CriterionEntity c1 = criterion(CriterionKind.BOOLEAN, CriterionRole.PRIMARY, "5.00", null, null, null);
        CriterionEntity c2 = criterion(CriterionKind.BOOLEAN, CriterionRole.PRIMARY, "5.00", null, null, null);
        RubricEntity r = rubric("10.00", false, List.of(c1, c2));
        assertThatThrownBy(() -> calculator.calculate(r, List.of(boolScore(c1.getId(), true))))
                .isInstanceOf(RubricInvariantViolation.class)
                .satisfies(e -> assertThat(((RubricInvariantViolation) e).getCode())
                        .isEqualTo("ASSESSMENT_SCORES_INCOMPLETE"));
    }

    @Test
    void duplicate_score_for_same_criterion_is_rejected() {
        CriterionEntity c = criterion(CriterionKind.BOOLEAN, CriterionRole.PRIMARY, "5.00", null, null, null);
        RubricEntity r = rubric("5.00", false, List.of(c));
        // size совпадает (1 == 1), но критерий внутри ассессмента не покрыт корректно
        // Воссоздадим через дубль:
        RubricEntity r2 = rubric("10.00", false, List.of(c, c));  // два указателя на одну криту
        assertThatThrownBy(() -> calculator.calculate(r2, List.of(
                boolScore(c.getId(), true),
                boolScore(c.getId(), false)
        )))
                .isInstanceOf(RubricInvariantViolation.class)
                .hasMessageContaining("Duplicate score");
    }

    @Test
    void writes_computedPoints_back_to_scores() {
        CriterionEntity c = criterion(CriterionKind.PERCENT, CriterionRole.PRIMARY, "10.00", null, null, null);
        RubricEntity r = rubric("10.00", false, List.of(c));
        CriterionScoreEntity s = percentScore(c.getId(), "80.00");
        calculator.calculate(r, List.of(s));
        assertThat(s.getComputedPoints()).isEqualByComparingTo("8.00");
    }
}

package com.example.lms.service.grading;

import com.example.lms.entity.grading.CriterionEntity;
import com.example.lms.entity.grading.CriterionKind;
import com.example.lms.entity.grading.CriterionRole;
import com.example.lms.entity.grading.CriterionScoreEntity;
import com.example.lms.entity.grading.RubricEntity;
import com.example.lms.exception.RubricInvariantViolation;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * TICKET-BE-33: чистая функция расчёта итогового балла по рубрике.
 * Stateless — single source of truth для BE/FE/iOS.
 */
@Component
public class RubricScoreCalculator {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final int POINTS_SCALE = 2;
    private static final int COEFFICIENT_SCALE = 4;

    /**
     * Рассчитывает итоговую оценку и computedPoints для каждого критерия.
     * Не модифицирует входные scores — копирует и возвращает новый список.
     */
    public CalculationResult calculate(RubricEntity rubric, List<CriterionScoreEntity> rawScores) {
        if (rawScores == null || rawScores.size() != rubric.getCriteria().size()) {
            throw new RubricInvariantViolation("ASSESSMENT_SCORES_INCOMPLETE",
                    "Number of scores must match number of criteria");
        }

        Map<UUID, CriterionEntity> byId = new HashMap<>();
        for (CriterionEntity c : rubric.getCriteria()) {
            byId.put(c.getId(), c);
        }

        Map<UUID, CriterionScoreEntity> seen = new HashMap<>();
        for (CriterionScoreEntity s : rawScores) {
            if (s.getCriterionId() == null) {
                throw new RubricInvariantViolation("ASSESSMENT_SCORES_INCOMPLETE", "criterionId is required");
            }
            if (!byId.containsKey(s.getCriterionId())) {
                throw new RubricInvariantViolation("ASSESSMENT_SCORES_INCOMPLETE",
                        "Unknown criterionId: " + s.getCriterionId());
            }
            if (seen.put(s.getCriterionId(), s) != null) {
                throw new RubricInvariantViolation("ASSESSMENT_SCORES_INCOMPLETE",
                        "Duplicate score for criterion: " + s.getCriterionId());
            }
        }

        BigDecimal primarySum = BigDecimal.ZERO;
        BigDecimal bonusDelta = BigDecimal.ZERO;

        for (CriterionEntity c : rubric.getCriteria()) {
            CriterionScoreEntity s = seen.get(c.getId());
            validateScoreShape(c, s);
            BigDecimal computed = computePoints(c, s);
            s.setComputedPoints(computed);

            if (c.getRole() == CriterionRole.PRIMARY) {
                primarySum = primarySum.add(computed);
            } else {
                bonusDelta = bonusDelta.add(computed);
            }
        }

        primarySum = primarySum.setScale(POINTS_SCALE, RoundingMode.HALF_UP);
        BigDecimal primarySumCapped = primarySum.min(rubric.getTotalMaxPoints());

        BigDecimal bonusMultiplier = ONE.add(bonusDelta).setScale(COEFFICIENT_SCALE, RoundingMode.HALF_UP);
        BigDecimal finalScoreRaw = primarySumCapped.multiply(bonusMultiplier)
                .setScale(POINTS_SCALE, RoundingMode.HALF_UP);

        BigDecimal finalScore;
        if (rubric.isAllowOvercap()) {
            finalScore = finalScoreRaw;
        } else {
            finalScore = finalScoreRaw.min(rubric.getTotalMaxPoints());
        }

        BigDecimal normalizedRaw = finalScore.multiply(HUNDRED)
                .divide(rubric.getTotalMaxPoints(), POINTS_SCALE, RoundingMode.HALF_UP);
        int normalized = normalizedRaw.setScale(0, RoundingMode.HALF_UP).intValue();
        if (normalized < 0) normalized = 0;
        if (normalized > 100) normalized = 100;

        return new CalculationResult(primarySum, bonusMultiplier, finalScore, (short) normalized);
    }

    private void validateScoreShape(CriterionEntity c, CriterionScoreEntity s) {
        switch (c.getKind()) {
            case BOOLEAN -> {
                if (s.getBoolValue() == null) {
                    throw new RubricInvariantViolation("ASSESSMENT_SCORE_TYPE_MISMATCH",
                            "BOOLEAN criterion requires boolValue");
                }
                if (s.getPercentValue() != null || s.getScoreValue() != null) {
                    throw new RubricInvariantViolation("ASSESSMENT_SCORE_TYPE_MISMATCH",
                            "BOOLEAN criterion must not have percent/score values");
                }
            }
            case PERCENT -> {
                if (s.getPercentValue() == null) {
                    throw new RubricInvariantViolation("ASSESSMENT_SCORE_TYPE_MISMATCH",
                            "PERCENT criterion requires percentValue");
                }
                if (s.getBoolValue() != null || s.getScoreValue() != null) {
                    throw new RubricInvariantViolation("ASSESSMENT_SCORE_TYPE_MISMATCH",
                            "PERCENT criterion must not have bool/score values");
                }
                if (s.getPercentValue().signum() < 0 || s.getPercentValue().compareTo(HUNDRED) > 0) {
                    throw new RubricInvariantViolation("ASSESSMENT_SCORE_OUT_OF_RANGE",
                            "percentValue must be in [0, 100]");
                }
            }
            case SCORE -> {
                if (s.getScoreValue() == null) {
                    throw new RubricInvariantViolation("ASSESSMENT_SCORE_TYPE_MISMATCH",
                            "SCORE criterion requires scoreValue");
                }
                if (s.getBoolValue() != null || s.getPercentValue() != null) {
                    throw new RubricInvariantViolation("ASSESSMENT_SCORE_TYPE_MISMATCH",
                            "SCORE criterion must not have bool/percent values");
                }
                if (s.getScoreValue().compareTo(c.getScoreMin()) < 0
                        || s.getScoreValue().compareTo(c.getScoreMax()) > 0) {
                    throw new RubricInvariantViolation("ASSESSMENT_SCORE_OUT_OF_RANGE",
                            "scoreValue must be in [" + c.getScoreMin() + ", " + c.getScoreMax() + "]");
                }
            }
        }
    }

    private BigDecimal computePoints(CriterionEntity c, CriterionScoreEntity s) {
        BigDecimal base = c.getRole() == CriterionRole.PRIMARY
                ? c.getMaxPoints()
                : c.getMaxCoefficient().subtract(ONE);
        int scale = c.getRole() == CriterionRole.PRIMARY ? POINTS_SCALE : COEFFICIENT_SCALE;

        BigDecimal fraction = switch (c.getKind()) {
            case BOOLEAN -> s.getBoolValue() ? ONE : BigDecimal.ZERO;
            case PERCENT -> s.getPercentValue().divide(HUNDRED, 10, RoundingMode.HALF_UP);
            case SCORE -> {
                BigDecimal range = c.getScoreMax().subtract(c.getScoreMin());
                BigDecimal delta = s.getScoreValue().subtract(c.getScoreMin());
                yield delta.divide(range, 10, RoundingMode.HALF_UP);
            }
        };

        return base.multiply(fraction).setScale(scale, RoundingMode.HALF_UP);
    }

    public record CalculationResult(
            BigDecimal primarySum,
            BigDecimal bonusMultiplier,
            BigDecimal finalScore,
            short finalScoreNormalized
    ) {}
}

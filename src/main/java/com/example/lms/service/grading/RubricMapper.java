package com.example.lms.service.grading;

import com.example.lms.dto.grading.CriterionDto;
import com.example.lms.dto.grading.RubricDto;
import com.example.lms.entity.grading.CriterionEntity;
import com.example.lms.entity.grading.RubricEntity;

import java.util.List;

/**
 * TICKET-BE-37: маппинг snapshot-рубрик в DTO.
 */
public final class RubricMapper {

    private RubricMapper() {}

    public static CriterionDto toDto(CriterionEntity entity) {
        return new CriterionDto(
                entity.getId(),
                entity.getOrdinal(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getKind(),
                entity.getRole(),
                entity.getMaxPoints(),
                entity.getMaxCoefficient(),
                entity.getScoreMin(),
                entity.getScoreMax()
        );
    }

    public static RubricDto toDto(RubricEntity entity) {
        List<CriterionDto> criteria = entity.getCriteria().stream()
                .map(RubricMapper::toDto)
                .toList();
        return new RubricDto(
                entity.getId(),
                entity.getAssignmentId(),
                entity.getSourceTemplateId(),
                entity.getName(),
                entity.getDescription(),
                entity.getTotalMaxPoints(),
                entity.isAllowOvercap(),
                entity.getFrozenAt(),
                criteria
        );
    }
}

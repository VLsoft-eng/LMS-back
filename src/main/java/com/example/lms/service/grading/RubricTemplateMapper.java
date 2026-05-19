package com.example.lms.service.grading;

import com.example.lms.dto.grading.CriterionTemplateDto;
import com.example.lms.dto.grading.CriterionTemplateInput;
import com.example.lms.dto.grading.RubricTemplateDto;
import com.example.lms.dto.grading.RubricTemplateShortDto;
import com.example.lms.entity.grading.CriterionTemplateEntity;
import com.example.lms.entity.grading.RubricTemplateEntity;

import java.util.List;

/**
 * TICKET-BE-36: маппинг между entity и DTO для рубрик-шаблонов.
 */
public final class RubricTemplateMapper {

    private RubricTemplateMapper() {}

    public static CriterionTemplateEntity toEntity(CriterionTemplateInput input) {
        return CriterionTemplateEntity.builder()
                .ordinal(input.ordinal())
                .title(input.title())
                .description(input.description())
                .kind(input.kind())
                .role(input.role())
                .maxPoints(input.maxPoints())
                .maxCoefficient(input.maxCoefficient())
                .scoreMin(input.scoreMin())
                .scoreMax(input.scoreMax())
                .build();
    }

    public static CriterionTemplateDto toDto(CriterionTemplateEntity entity) {
        return new CriterionTemplateDto(
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

    public static RubricTemplateDto toDto(RubricTemplateEntity entity) {
        List<CriterionTemplateDto> criteria = entity.getCriteria().stream()
                .map(RubricTemplateMapper::toDto)
                .toList();
        return new RubricTemplateDto(
                entity.getId(),
                entity.getClassId(),
                entity.getName(),
                entity.getDescription(),
                entity.getTotalMaxPoints(),
                entity.isAllowOvercap(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                criteria
        );
    }

    public static RubricTemplateShortDto toShortDto(RubricTemplateEntity entity) {
        return new RubricTemplateShortDto(
                entity.getId(),
                entity.getName(),
                entity.getTotalMaxPoints(),
                entity.getCriteria().size(),
                entity.getCreatedAt()
        );
    }
}

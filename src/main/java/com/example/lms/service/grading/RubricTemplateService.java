package com.example.lms.service.grading;

import com.example.lms.dto.grading.CreateRubricTemplateRequest;
import com.example.lms.dto.grading.CriterionTemplateInput;
import com.example.lms.dto.grading.RubricTemplateDto;
import com.example.lms.dto.grading.RubricTemplateShortDto;
import com.example.lms.dto.grading.UpdateRubricTemplateRequest;
import com.example.lms.entity.grading.CriterionTemplateEntity;
import com.example.lms.entity.grading.RubricTemplateEntity;
import com.example.lms.entity.UserEntity;
import com.example.lms.exception.ResourceNotFoundException;
import com.example.lms.exception.RubricConflictException;
import com.example.lms.repository.RubricRepository;
import com.example.lms.repository.RubricTemplateRepository;
import com.example.lms.service.ClassSecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * TICKET-BE-36: бизнес-логика управления шаблонами рубрик.
 */
@Service
@RequiredArgsConstructor
public class RubricTemplateService {

    private final RubricTemplateRepository rubricTemplateRepository;
    private final RubricRepository rubricRepository;
    private final ClassSecurityService classSecurityService;

    @Transactional
    public RubricTemplateDto create(UUID classId, CreateRubricTemplateRequest request, UserEntity currentUser) {
        classSecurityService.requireOwnerOrTeacher(classId, currentUser.getId());

        RubricTemplateEntity entity = RubricTemplateEntity.builder()
                .classId(classId)
                .name(request.name())
                .description(request.description())
                .totalMaxPoints(request.totalMaxPoints())
                .allowOvercap(request.allowOvercap())
                .createdBy(currentUser.getId())
                .criteria(mapCriteria(request.criteria()))
                .build();

        entity.validateInvariants();
        entity = rubricTemplateRepository.save(entity);
        return RubricTemplateMapper.toDto(entity);
    }

    @Transactional(readOnly = true)
    public List<RubricTemplateShortDto> listByClass(UUID classId, UUID currentUserId) {
        classSecurityService.requireMember(classId, currentUserId);
        return rubricTemplateRepository.findAllByClassIdOrderByCreatedAtDesc(classId).stream()
                .map(RubricTemplateMapper::toShortDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public RubricTemplateDto getById(UUID id, UUID currentUserId) {
        RubricTemplateEntity entity = rubricTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rubric template not found: " + id));
        classSecurityService.requireMember(entity.getClassId(), currentUserId);
        return RubricTemplateMapper.toDto(entity);
    }

    @Transactional
    public RubricTemplateDto update(UUID id, UpdateRubricTemplateRequest request, UserEntity currentUser) {
        RubricTemplateEntity entity = rubricTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rubric template not found: " + id));

        classSecurityService.requireOwnerOrTeacher(entity.getClassId(), currentUser.getId());

        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setTotalMaxPoints(request.totalMaxPoints());
        entity.setAllowOvercap(request.allowOvercap());
        entity.getCriteria().clear();
        entity.getCriteria().addAll(mapCriteria(request.criteria()));

        entity.validateInvariants();
        entity = rubricTemplateRepository.save(entity);
        return RubricTemplateMapper.toDto(entity);
    }

    @Transactional
    public void delete(UUID id, UserEntity currentUser) {
        RubricTemplateEntity entity = rubricTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rubric template not found: " + id));

        classSecurityService.requireOwnerOrTeacher(entity.getClassId(), currentUser.getId());

        if (rubricRepository.existsBySourceTemplateId(id)) {
            throw new RubricConflictException("RUBRIC_TEMPLATE_IN_USE",
                    "Template is used by at least one rubric snapshot");
        }

        rubricTemplateRepository.delete(entity);
    }

    /**
     * Возвращает entity с прогруженными критериями. Используется в RubricAttachmentService.
     */
    @Transactional(readOnly = true)
    public RubricTemplateEntity requireById(UUID id) {
        return rubricTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rubric template not found: " + id));
    }

    private List<CriterionTemplateEntity> mapCriteria(List<CriterionTemplateInput> inputs) {
        List<CriterionTemplateEntity> list = new ArrayList<>();
        for (CriterionTemplateInput input : inputs) {
            list.add(RubricTemplateMapper.toEntity(input));
        }
        return list;
    }
}

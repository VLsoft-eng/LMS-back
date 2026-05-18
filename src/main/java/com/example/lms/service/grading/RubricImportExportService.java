package com.example.lms.service.grading;

import com.example.lms.dto.grading.CreateRubricTemplateRequest;
import com.example.lms.dto.grading.RubricExportPayload;
import com.example.lms.dto.grading.RubricTemplateDto;
import com.example.lms.entity.UserEntity;
import com.example.lms.entity.grading.RubricTemplateEntity;
import com.example.lms.exception.ResourceNotFoundException;
import com.example.lms.exception.RubricInvariantViolation;
import com.example.lms.repository.RubricTemplateRepository;
import com.example.lms.service.ClassSecurityService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * TICKET-BE-36: импорт/экспорт шаблонов рубрик (JSON, формат §5 PRD).
 */
@Service
@RequiredArgsConstructor
public class RubricImportExportService {

    public static final String CURRENT_VERSION = "1.0";
    public static final String SCHEMA_URL = "https://lms.example.com/schemas/rubric-template-v1.json";
    public static final int MAX_IMPORT_BYTES = 256 * 1024;
    private static final Set<String> SUPPORTED_VERSIONS = Set.of("1.0");

    private final RubricTemplateRepository rubricTemplateRepository;
    private final ClassSecurityService classSecurityService;
    private final RubricTemplateService rubricTemplateService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public RubricExportPayload export(UUID templateId, UUID currentUserId) {
        RubricTemplateEntity entity = rubricTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Rubric template not found: " + templateId));

        classSecurityService.requireOwnerOrTeacher(entity.getClassId(), currentUserId);

        var criteria = entity.getCriteria().stream()
                .map(c -> new com.example.lms.dto.grading.CriterionTemplateInput(
                        c.getOrdinal(), c.getTitle(), c.getDescription(),
                        c.getKind(), c.getRole(),
                        c.getMaxPoints(), c.getMaxCoefficient(),
                        c.getScoreMin(), c.getScoreMax()
                ))
                .toList();

        return new RubricExportPayload(
                SCHEMA_URL,
                CURRENT_VERSION,
                Instant.now(),
                new RubricExportPayload.RubricExportBody(
                        entity.getName(),
                        entity.getDescription(),
                        entity.getTotalMaxPoints(),
                        entity.isAllowOvercap(),
                        criteria
                )
        );
    }

    /**
     * Импорт шаблона из бинарного JSON-payload (из multipart-файла).
     * Размер не должен превышать {@link #MAX_IMPORT_BYTES}.
     */
    @Transactional
    public RubricTemplateDto importFromBytes(UUID classId, byte[] payloadBytes, UserEntity currentUser) {
        if (payloadBytes == null || payloadBytes.length == 0) {
            throw new RubricInvariantViolation("RUBRIC_IMPORT_INVALID_SCHEMA", "Empty payload");
        }
        if (payloadBytes.length > MAX_IMPORT_BYTES) {
            throw new RubricInvariantViolation("RUBRIC_IMPORT_INVALID_SCHEMA",
                    "Payload exceeds " + MAX_IMPORT_BYTES + " bytes");
        }
        RubricExportPayload payload;
        try {
            payload = objectMapper.readValue(payloadBytes, RubricExportPayload.class);
        } catch (IOException e) {
            throw new RubricInvariantViolation("RUBRIC_IMPORT_INVALID_SCHEMA",
                    "Invalid JSON: " + e.getMessage());
        }
        return importFromPayload(classId, payload, currentUser);
    }

    @Transactional
    public RubricTemplateDto importFromPayload(UUID classId, RubricExportPayload payload, UserEntity currentUser) {
        if (payload == null) {
            throw new RubricInvariantViolation("RUBRIC_IMPORT_INVALID_SCHEMA", "Payload is null");
        }
        if (payload.version() == null) {
            throw new RubricInvariantViolation("RUBRIC_IMPORT_INVALID_SCHEMA", "version field is required");
        }
        if (!SUPPORTED_VERSIONS.contains(payload.version())) {
            throw new RubricInvariantViolation("RUBRIC_IMPORT_VERSION_UNSUPPORTED",
                    "Unsupported version: " + payload.version());
        }
        if (payload.rubric() == null) {
            throw new RubricInvariantViolation("RUBRIC_IMPORT_INVALID_SCHEMA", "rubric field is required");
        }

        var body = payload.rubric();
        var request = new CreateRubricTemplateRequest(
                body.name(), body.description(),
                body.totalMaxPoints(), body.allowOvercap(),
                body.criteria() == null ? List.of() : body.criteria()
        );
        return rubricTemplateService.create(classId, request, currentUser);
    }

    /**
     * Сериализует export-payload в JSON-байты для скачивания.
     */
    public byte[] serialize(RubricExportPayload payload) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize rubric export", e);
        }
    }

    public String slug(String name) {
        if (name == null) return "rubric";
        String slug = name.toLowerCase().replaceAll("[^a-z0-9а-яё\\-]+", "-").replaceAll("(^-+|-+$)", "");
        if (slug.isEmpty()) return "rubric";
        return new String(slug.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }
}

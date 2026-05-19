package com.example.lms.controller;

import com.example.lms.dto.grading.CreateRubricTemplateRequest;
import com.example.lms.dto.grading.RubricExportPayload;
import com.example.lms.dto.grading.RubricTemplateDto;
import com.example.lms.dto.grading.RubricTemplateShortDto;
import com.example.lms.dto.grading.UpdateRubricTemplateRequest;
import com.example.lms.entity.UserEntity;
import com.example.lms.security.CurrentUser;
import com.example.lms.service.grading.RubricImportExportService;
import com.example.lms.service.grading.RubricTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * TICKET-BE-39: REST-эндпойнты шаблонов рубрик (1–7 из §4.5).
 */
@Tag(name = "Шаблоны рубрик", description = "CRUD и импорт/экспорт шаблонов критериев на уровне класса")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RubricTemplateController {

    private final RubricTemplateService rubricTemplateService;
    private final RubricImportExportService importExportService;

    @Operation(summary = "Создать шаблон рубрики", description = "OWNER/TEACHER класса")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Шаблон создан"),
            @ApiResponse(responseCode = "400", description = "Нарушен инвариант (RUBRIC_PRIMARY_SUM_MISMATCH / RUBRIC_CRITERION_INVALID)"),
            @ApiResponse(responseCode = "403", description = "Только OWNER/TEACHER")
    })
    @PostMapping("/classes/{classId}/rubric-templates")
    @ResponseStatus(HttpStatus.CREATED)
    public RubricTemplateDto create(@PathVariable UUID classId,
                                    @Valid @RequestBody CreateRubricTemplateRequest request,
                                    @CurrentUser UserEntity currentUser) {
        return rubricTemplateService.create(classId, request, currentUser);
    }

    @Operation(summary = "Список шаблонов класса")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Не участник класса")
    })
    @GetMapping("/classes/{classId}/rubric-templates")
    public List<RubricTemplateShortDto> listByClass(@PathVariable UUID classId,
                                                     @CurrentUser UserEntity currentUser) {
        return rubricTemplateService.listByClass(classId, currentUser.getId());
    }

    @Operation(summary = "Получить шаблон по ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Не участник класса"),
            @ApiResponse(responseCode = "404", description = "Шаблон не найден")
    })
    @GetMapping("/rubric-templates/{id}")
    public RubricTemplateDto getById(@PathVariable UUID id,
                                      @CurrentUser UserEntity currentUser) {
        return rubricTemplateService.getById(id, currentUser.getId());
    }

    @Operation(summary = "Обновить шаблон (полная замена)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "400", description = "Нарушен инвариант"),
            @ApiResponse(responseCode = "403", description = "Только OWNER/TEACHER"),
            @ApiResponse(responseCode = "404", description = "Шаблон не найден")
    })
    @PutMapping("/rubric-templates/{id}")
    public RubricTemplateDto update(@PathVariable UUID id,
                                     @Valid @RequestBody UpdateRubricTemplateRequest request,
                                     @CurrentUser UserEntity currentUser) {
        return rubricTemplateService.update(id, request, currentUser);
    }

    @Operation(summary = "Удалить шаблон")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Только OWNER/TEACHER"),
            @ApiResponse(responseCode = "404", description = "Шаблон не найден"),
            @ApiResponse(responseCode = "409", description = "RUBRIC_TEMPLATE_IN_USE")
    })
    @DeleteMapping("/rubric-templates/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @CurrentUser UserEntity currentUser) {
        rubricTemplateService.delete(id, currentUser);
    }

    @Operation(summary = "Экспорт шаблона в JSON", description = "Скачивание файла")
    @GetMapping("/rubric-templates/{id}/export")
    public ResponseEntity<byte[]> export(@PathVariable UUID id,
                                          @CurrentUser UserEntity currentUser) {
        RubricExportPayload payload = importExportService.export(id, currentUser.getId());
        byte[] body = importExportService.serialize(payload);
        String filename = "rubric-" + importExportService.slug(payload.rubric().name()) + ".json";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(body);
    }

    @Operation(summary = "Импорт шаблона из JSON",
            description = "Принимает multipart/form-data (file) или application/json")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Шаблон создан"),
            @ApiResponse(responseCode = "400",
                    description = "RUBRIC_IMPORT_INVALID_SCHEMA / RUBRIC_IMPORT_VERSION_UNSUPPORTED"),
            @ApiResponse(responseCode = "403", description = "Только OWNER/TEACHER")
    })
    @PostMapping(value = "/classes/{classId}/rubric-templates/import",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public RubricTemplateDto importMultipart(@PathVariable UUID classId,
                                              @RequestParam("file") MultipartFile file,
                                              @CurrentUser UserEntity currentUser) throws IOException {
        return importExportService.importFromBytes(classId, file.getBytes(), currentUser);
    }

    @PostMapping(value = "/classes/{classId}/rubric-templates/import",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public RubricTemplateDto importJson(@PathVariable UUID classId,
                                         @RequestBody RubricExportPayload payload,
                                         @CurrentUser UserEntity currentUser) {
        return importExportService.importFromPayload(classId, payload, currentUser);
    }
}

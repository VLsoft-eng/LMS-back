package com.example.lms.controller;

import com.example.lms.dto.AssignmentDetailDto;
import com.example.lms.dto.AssignmentDto;
import com.example.lms.dto.CreateAssignmentRequest;
import com.example.lms.entity.UserEntity;
import com.example.lms.security.CurrentUser;
import com.example.lms.service.AssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Tag(name = "Задания", description = "Задания класса")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    @Operation(summary = "Список заданий", description = "Задания класса. Для студента — с submissionStatus.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация"),
            @ApiResponse(responseCode = "403", description = "Нет доступа к классу"),
            @ApiResponse(responseCode = "404", description = "Класс не найден")
    })
    @GetMapping("/classes/{classId}/assignments")
    public Page<AssignmentDto> getAssignments(@PathVariable UUID classId,
                                              @CurrentUser UserEntity currentUser,
                                              @PageableDefault(size = 20, sort = "createdAt")
                                              @Parameter(description = "Пагинация: page (с 0), size, sort (поле,asc|desc)", example = "page=0&size=20")
                                              Pageable pageable) {
        return assignmentService.getAssignments(classId, currentUser.getId(), pageable);
    }

    @Operation(summary = "Создать задание", description = "Создание задания. OWNER и TEACHER.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Задание создано"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация"),
            @ApiResponse(responseCode = "403", description = "Только OWNER/TEACHER могут создавать задания"),
            @ApiResponse(responseCode = "404", description = "Класс не найден")
    })
    @PostMapping(value = "/classes/{classId}/assignments", consumes = {"multipart/form-data", "application/json"})
    @ResponseStatus(HttpStatus.CREATED)
    public AssignmentDto createAssignment(@PathVariable UUID classId,
                                         @RequestBody(required = false) @Valid CreateAssignmentRequest jsonRequest,
                                         @RequestPart(value = "title", required = false) String title,
                                         @RequestPart(value = "description", required = false) String description,
                                         @RequestPart(value = "deadline", required = false) String deadline,
                                         @RequestParam(value = "files", required = false) List<MultipartFile> files,
                                         @CurrentUser UserEntity currentUser) {
        String finalTitle;
        String finalDescription;
        Instant deadlineInstant;

        if (jsonRequest != null) {
            finalTitle = jsonRequest.title();
            finalDescription = jsonRequest.description() != null ? jsonRequest.description() : "";
            deadlineInstant = jsonRequest.deadline();
        } else {
            if (title == null || title.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required");
            }
            finalTitle = title;
            finalDescription = description != null ? description : "";
            deadlineInstant = (deadline != null && !deadline.isBlank())
                    ? Instant.parse(deadline)
                    : null;
        }
        return assignmentService.createAssignment(classId, finalTitle, finalDescription,
                deadlineInstant, files, currentUser);
    }

    @Operation(summary = "Получить задание", description = "Детали задания по ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация"),
            @ApiResponse(responseCode = "403", description = "Нет доступа"),
            @ApiResponse(responseCode = "404", description = "Задание не найдено")
    })
    @GetMapping("/assignments/{id}")
    public AssignmentDetailDto getAssignment(@PathVariable("id") UUID assignmentId,
                                            @CurrentUser UserEntity currentUser) {
        return assignmentService.getAssignment(assignmentId, currentUser.getId());
    }
}

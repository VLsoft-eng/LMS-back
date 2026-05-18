package com.example.lms.controller;

import com.example.lms.dto.grading.AttachRubricRequest;
import com.example.lms.dto.grading.RubricDto;
import com.example.lms.entity.UserEntity;
import com.example.lms.security.CurrentUser;
import com.example.lms.service.grading.RubricAttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * TICKET-BE-40: привязка/открепление рубрики к заданию (эндпойнты 8–10 §4.5).
 */
@Tag(name = "Рубрика задания", description = "Привязка/получение/открепление рубрики у задания")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RubricController {

    private final RubricAttachmentService rubricAttachmentService;

    @Operation(summary = "Привязать рубрику к заданию", description = "Из шаблона или ad-hoc. OWNER/TEACHER")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Рубрика привязана"),
            @ApiResponse(responseCode = "400", description = "RUBRIC_BODY_MUTUALLY_EXCLUSIVE / нарушение инвариантов"),
            @ApiResponse(responseCode = "403", description = "Только OWNER/TEACHER"),
            @ApiResponse(responseCode = "404", description = "Задание/шаблон не найден"),
            @ApiResponse(responseCode = "409",
                    description = "RUBRIC_ALREADY_ATTACHED / ASSIGNMENT_HAS_GRADES")
    })
    @PostMapping("/assignments/{assignmentId}/rubric")
    @ResponseStatus(HttpStatus.CREATED)
    public RubricDto attach(@PathVariable UUID assignmentId,
                             @Valid @RequestBody AttachRubricRequest request,
                             @CurrentUser UserEntity currentUser) {
        return rubricAttachmentService.attach(assignmentId, request, currentUser);
    }

    @Operation(summary = "Получить рубрику задания")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Не участник класса"),
            @ApiResponse(responseCode = "404", description = "Рубрика не привязана")
    })
    @GetMapping("/assignments/{assignmentId}/rubric")
    public RubricDto getByAssignment(@PathVariable UUID assignmentId,
                                      @CurrentUser UserEntity currentUser) {
        return rubricAttachmentService.getByAssignment(assignmentId, currentUser.getId());
    }

    @Operation(summary = "Открепить рубрику от задания")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Только OWNER/TEACHER"),
            @ApiResponse(responseCode = "404", description = "Рубрика не привязана"),
            @ApiResponse(responseCode = "409", description = "RUBRIC_HAS_ASSESSMENTS")
    })
    @DeleteMapping("/assignments/{assignmentId}/rubric")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void detach(@PathVariable UUID assignmentId,
                        @CurrentUser UserEntity currentUser) {
        rubricAttachmentService.detach(assignmentId, currentUser);
    }
}

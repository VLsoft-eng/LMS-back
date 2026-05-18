package com.example.lms.controller;

import com.example.lms.dto.grading.AssessmentDto;
import com.example.lms.dto.grading.CreateAssessmentRequest;
import com.example.lms.dto.grading.MyAssessmentDto;
import com.example.lms.dto.grading.UpdateAssessmentRequest;
import com.example.lms.entity.UserEntity;
import com.example.lms.security.CurrentUser;
import com.example.lms.service.grading.AssessmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * TICKET-BE-41: REST-эндпойнты ассессментов (11–17 §4.5).
 */
@Tag(name = "Ассессменты", description = "Оценивание сабмишена/командной оценки по рубрике")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentService assessmentService;

    @Operation(summary = "Создать ассессмент",
            description = "Один из submissionId/teamGradeId. OWNER/TEACHER")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ассессмент создан"),
            @ApiResponse(responseCode = "400",
                    description = "ASSESSMENT_SCORES_INCOMPLETE / ASSESSMENT_SCORE_TYPE_MISMATCH / ASSESSMENT_SCORE_OUT_OF_RANGE"),
            @ApiResponse(responseCode = "403", description = "Только OWNER/TEACHER"),
            @ApiResponse(responseCode = "404", description = "Рубрика/сабмишен не найдены"),
            @ApiResponse(responseCode = "409",
                    description = "ASSESSMENT_TARGET_MUTUALLY_EXCLUSIVE / ASSESSMENT_ALREADY_EXISTS")
    })
    @PostMapping("/assignments/{assignmentId}/assessments")
    @ResponseStatus(HttpStatus.CREATED)
    public AssessmentDto create(@PathVariable UUID assignmentId,
                                 @Valid @RequestBody CreateAssessmentRequest request,
                                 @CurrentUser UserEntity currentUser) {
        return assessmentService.create(assignmentId, request, currentUser);
    }

    @Operation(summary = "Получить ассессмент по ID")
    @GetMapping("/assessments/{assessmentId}")
    public AssessmentDto getById(@PathVariable UUID assessmentId,
                                  @CurrentUser UserEntity currentUser) {
        return assessmentService.getById(assessmentId, currentUser);
    }

    @Operation(summary = "Обновить ассессмент (пересчёт)")
    @PutMapping("/assessments/{assessmentId}")
    public AssessmentDto update(@PathVariable UUID assessmentId,
                                 @Valid @RequestBody UpdateAssessmentRequest request,
                                 @CurrentUser UserEntity currentUser) {
        return assessmentService.update(assessmentId, request, currentUser);
    }

    @Operation(summary = "Удалить ассессмент")
    @DeleteMapping("/assessments/{assessmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID assessmentId,
                        @CurrentUser UserEntity currentUser) {
        assessmentService.delete(assessmentId, currentUser);
    }

    @Operation(summary = "Ассессмент по сабмишену")
    @GetMapping("/submissions/{submissionId}/assessment")
    public AssessmentDto getBySubmission(@PathVariable UUID submissionId,
                                          @CurrentUser UserEntity currentUser) {
        return assessmentService.getBySubmission(submissionId, currentUser);
    }

    @Operation(summary = "Ассессмент по командной оценке")
    @GetMapping("/team-grades/{teamGradeId}/assessment")
    public AssessmentDto getByTeamGrade(@PathVariable UUID teamGradeId,
                                         @CurrentUser UserEntity currentUser) {
        return assessmentService.getByTeamGrade(teamGradeId, currentUser);
    }

    @Operation(summary = "Мои ассессменты (студент)")
    @GetMapping("/submissions/my/assessments")
    public List<MyAssessmentDto> listMyAssessments(@CurrentUser UserEntity currentUser) {
        return assessmentService.listMyAssessments(currentUser.getId());
    }
}

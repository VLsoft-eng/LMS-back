package com.example.lms.controller;

import com.example.lms.dto.*;
import com.example.lms.entity.UserEntity;
import com.example.lms.security.CurrentUser;
import com.example.lms.service.TeamGradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * TICKET-BE-29: REST-эндпоинты групповых оценок и корректировок.
 */
@Tag(name = "Групповые оценки", description = "Оценки команд и индивидуальные корректировки")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TeamGradeController {

    private final TeamGradeService teamGradeService;

    @Operation(summary = "Выставить групповую оценку")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Оценка создана"),
            @ApiResponse(responseCode = "400", description = "Оценка вне 0-100"),
            @ApiResponse(responseCode = "403", description = "Только OWNER/TEACHER"),
            @ApiResponse(responseCode = "404", description = "Задание или команда не найдена"),
            @ApiResponse(responseCode = "409", description = "Команда уже оценена")
    })
    @PostMapping("/assignments/{assignmentId}/team-grades")
    @ResponseStatus(HttpStatus.CREATED)
    public TeamGradeDto createTeamGrade(@PathVariable UUID assignmentId,
                                        @Valid @RequestBody CreateTeamGradeRequest request,
                                        @CurrentUser UserEntity currentUser) {
        return teamGradeService.createTeamGrade(assignmentId, request, currentUser);
    }

    @Operation(summary = "Список групповых оценок за задание")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Не участник класса")
    })
    @GetMapping("/assignments/{assignmentId}/team-grades")
    public Page<TeamGradeListItemDto> getTeamGrades(@PathVariable UUID assignmentId,
                                                     @CurrentUser UserEntity currentUser,
                                                     @PageableDefault(size = 20) Pageable pageable) {
        return teamGradeService.getTeamGrades(assignmentId, currentUser.getId(), pageable);
    }

    @Operation(summary = "Обновить групповую оценку")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Только OWNER/TEACHER"),
            @ApiResponse(responseCode = "404", description = "Оценка не найдена")
    })
    @PutMapping("/assignments/{assignmentId}/team-grades/{teamGradeId}")
    public TeamGradeDto updateTeamGrade(@PathVariable UUID assignmentId,
                                        @PathVariable UUID teamGradeId,
                                        @Valid @RequestBody UpdateTeamGradeRequest request,
                                        @CurrentUser UserEntity currentUser) {
        return teamGradeService.updateTeamGrade(assignmentId, teamGradeId, request, currentUser);
    }

    @Operation(summary = "Список индивидуальных корректировок")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Не участник класса")
    })
    @GetMapping("/assignments/{assignmentId}/team-grades/{teamGradeId}/adjustments")
    public List<IndividualAdjustmentDto> getAdjustments(@PathVariable UUID assignmentId,
                                                         @PathVariable UUID teamGradeId,
                                                         @CurrentUser UserEntity currentUser) {
        return teamGradeService.getAdjustments(assignmentId, teamGradeId, currentUser.getId());
    }

    @Operation(summary = "Корректировать индивидуальную оценку")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "400", description = "Корректировка вне -50..+50"),
            @ApiResponse(responseCode = "403", description = "Только OWNER/TEACHER"),
            @ApiResponse(responseCode = "404", description = "Не найдено")
    })
    @PutMapping("/assignments/{assignmentId}/team-grades/{teamGradeId}/adjustments/{studentId}")
    public IndividualAdjustmentDto updateAdjustment(@PathVariable UUID assignmentId,
                                                     @PathVariable UUID teamGradeId,
                                                     @PathVariable UUID studentId,
                                                     @Valid @RequestBody UpdateAdjustmentRequest request,
                                                     @CurrentUser UserEntity currentUser) {
        return teamGradeService.updateAdjustment(assignmentId, teamGradeId, studentId, request, currentUser);
    }

    @Operation(summary = "Моя командная оценка (для студента)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "404", description = "Оценка ещё не выставлена")
    })
    @GetMapping("/assignments/{assignmentId}/my-team-grade")
    public MyTeamGradeDto getMyTeamGrade(@PathVariable UUID assignmentId,
                                          @CurrentUser UserEntity currentUser) {
        return teamGradeService.getMyTeamGrade(assignmentId, currentUser.getId());
    }
}

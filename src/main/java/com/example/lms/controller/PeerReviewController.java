package com.example.lms.controller;

import com.example.lms.dto.peerreview.ConfigurePeerReviewRequest;
import com.example.lms.dto.peerreview.PeerAssessmentDto;
import com.example.lms.dto.peerreview.PeerReviewAssignmentDto;
import com.example.lms.dto.peerreview.PeerReviewResultDto;
import com.example.lms.dto.peerreview.PeerReviewSettingsDto;
import com.example.lms.dto.peerreview.SubmitPeerAssessmentRequest;
import com.example.lms.entity.UserEntity;
import com.example.lms.security.CurrentUser;
import com.example.lms.service.peerreview.PeerAssessmentService;
import com.example.lms.service.peerreview.PeerReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * TICKET #9184: REST-эндпоинты для peer-review (конфигурация, распределение, просмотр).
 *
 * <p>Эндпоинты уровня задания:
 * <ul>
 *   <li>POST /assignments/{id}/peer-review — включить/настроить (OWNER/TEACHER)
 *   <li>GET  /assignments/{id}/peer-review — получить настройки (member)
 *   <li>POST /assignments/{id}/peer-review/distribute — распределить рецензентов (OWNER/TEACHER)
 *   <li>GET  /assignments/{id}/peer-review/assignments — все назначения (OWNER/TEACHER)
 *   <li>GET  /assignments/{id}/peer-review/results — агрегированные результаты (OWNER/TEACHER)
 *   <li>GET  /assignments/{id}/peer-review/my-assignments — мои назначения (student)
 *   <li>GET  /assignments/{id}/peer-review/my-received — полученные оценки (student)
 * </ul>
 *
 * <p>Эндпоинты уровня назначения (/peer-review-assignments/):
 * <ul>
 *   <li>GET  /peer-review-assignments/{id} — детали назначения
 *   <li>POST /peer-review-assignments/{id}/assessment — отправить оценку
 *   <li>PUT  /peer-review-assignments/{id}/assessment — обновить оценку
 * </ul>
 */
@Tag(name = "Peer Review", description = "Взаимное оценивание работ студентами")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PeerReviewController {

    private final PeerReviewService peerReviewService;
    private final PeerAssessmentService peerAssessmentService;

    // ── Assignment-level endpoints ──────────────────────────────────────────

    @Operation(summary = "Настроить peer-review для задания",
            description = "Включает peer-review и задаёт количество рецензентов. OWNER/TEACHER.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Настройки сохранены"),
            @ApiResponse(responseCode = "403", description = "Только OWNER/TEACHER"),
            @ApiResponse(responseCode = "409", description = "PEER_REVIEW_NO_RUBRIC — рубрика не прикреплена")
    })
    @PostMapping("/assignments/{assignmentId}/peer-review")
    @ResponseStatus(HttpStatus.CREATED)
    public PeerReviewSettingsDto configure(
            @PathVariable UUID assignmentId,
            @Valid @RequestBody ConfigurePeerReviewRequest request,
            @CurrentUser UserEntity currentUser) {
        return peerReviewService.configure(assignmentId, request, currentUser);
    }

    @Operation(summary = "Получить настройки peer-review")
    @GetMapping("/assignments/{assignmentId}/peer-review")
    public PeerReviewSettingsDto getSettings(
            @PathVariable UUID assignmentId,
            @CurrentUser UserEntity currentUser) {
        return peerReviewService.getSettings(assignmentId, currentUser);
    }

    @Operation(summary = "Распределить рецензентов",
            description = "Назначает студентов-рецензентов по алгоритму round-robin. Идемпотентен. OWNER/TEACHER.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Назначения созданы/обновлены"),
            @ApiResponse(responseCode = "409", description = "PEER_REVIEW_NOT_ENOUGH_SUBMISSIONS")
    })
    @PostMapping("/assignments/{assignmentId}/peer-review/distribute")
    public List<PeerReviewAssignmentDto> distribute(
            @PathVariable UUID assignmentId,
            @CurrentUser UserEntity currentUser) {
        return peerReviewService.distributeReviewers(assignmentId, currentUser);
    }

    @Operation(summary = "Все назначения на проверку (учитель)")
    @GetMapping("/assignments/{assignmentId}/peer-review/assignments")
    public List<PeerReviewAssignmentDto> getAllAssignments(
            @PathVariable UUID assignmentId,
            @CurrentUser UserEntity currentUser) {
        return peerReviewService.getAllAssignments(assignmentId, currentUser);
    }

    @Operation(summary = "Агрегированные результаты peer-review (учитель)")
    @GetMapping("/assignments/{assignmentId}/peer-review/results")
    public List<PeerReviewResultDto> getResults(
            @PathVariable UUID assignmentId,
            @CurrentUser UserEntity currentUser) {
        return peerAssessmentService.getAssignmentResults(assignmentId, currentUser);
    }

    @Operation(summary = "Мои назначения на проверку (студент)")
    @GetMapping("/assignments/{assignmentId}/peer-review/my-assignments")
    public List<PeerReviewAssignmentDto> getMyAssignedReviews(
            @PathVariable UUID assignmentId,
            @CurrentUser UserEntity currentUser) {
        return peerAssessmentService.getMyAssignedReviews(assignmentId, currentUser);
    }

    @Operation(summary = "Полученные peer-оценки (студент, анонимно)")
    @GetMapping("/assignments/{assignmentId}/peer-review/my-received")
    public com.example.lms.dto.peerreview.MyReceivedPeerAssessmentsDto getMyReceived(
            @PathVariable UUID assignmentId,
            @CurrentUser UserEntity currentUser) {
        return peerAssessmentService.getMyReceivedAssessments(assignmentId, currentUser);
    }

    // ── PeerReviewAssignment-level endpoints ────────────────────────────────

    @Operation(summary = "Детали конкретного peer-review назначения")
    @GetMapping("/peer-review-assignments/{praId}")
    public PeerReviewAssignmentDto getAssignment(
            @PathVariable UUID praId,
            @CurrentUser UserEntity currentUser) {
        return peerAssessmentService.getMyAssignment(praId, currentUser);
    }

    @Operation(summary = "Отправить peer-оценку",
            description = "Студент-рецензент выставляет баллы по критериям рубрики задания.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Оценка принята"),
            @ApiResponse(responseCode = "403", description = "Не назначенный рецензент / самооценка"),
            @ApiResponse(responseCode = "409", description = "PEER_ASSESSMENT_ALREADY_EXISTS")
    })
    @PostMapping("/peer-review-assignments/{praId}/assessment")
    @ResponseStatus(HttpStatus.CREATED)
    public PeerAssessmentDto submitAssessment(
            @PathVariable UUID praId,
            @Valid @RequestBody SubmitPeerAssessmentRequest request,
            @CurrentUser UserEntity currentUser) {
        return peerAssessmentService.submitAssessment(praId, request, currentUser);
    }

    @Operation(summary = "Обновить peer-оценку")
    @PutMapping("/peer-review-assignments/{praId}/assessment")
    public PeerAssessmentDto updateAssessment(
            @PathVariable UUID praId,
            @Valid @RequestBody SubmitPeerAssessmentRequest request,
            @CurrentUser UserEntity currentUser) {
        return peerAssessmentService.updateAssessment(praId, request, currentUser);
    }
}

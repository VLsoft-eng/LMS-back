package com.example.lms.controller;

import com.example.lms.dto.GradeRequest;
import com.example.lms.dto.SubmissionDto;
import com.example.lms.entity.UserEntity;
import com.example.lms.security.CurrentUser;
import com.example.lms.service.SubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Tag(name = "Ответы", description = "Сдача ответов студентами и оценивание")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    @Operation(summary = "Сдать ответ", description = "Студент сдаёт ответ на задание (multipart, upsert)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ответ принят"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация"),
            @ApiResponse(responseCode = "403", description = "Только STUDENT может сдавать"),
            @ApiResponse(responseCode = "404", description = "Задание не найдено")
    })
    @PostMapping(value = "/assignments/{assignmentId}/submissions", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public SubmissionDto submit(@PathVariable UUID assignmentId,
                                @RequestPart(value = "answerText", required = false) String answerText,
                                @RequestParam(value = "files", required = false) List<MultipartFile> files,
                                @CurrentUser UserEntity currentUser) {
        return submissionService.submit(assignmentId, answerText, files, currentUser);
    }

    @Operation(summary = "Все ответы", description = "Просмотр всех ответов на задание (OWNER/TEACHER)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация"),
            @ApiResponse(responseCode = "403", description = "Только OWNER/TEACHER"),
            @ApiResponse(responseCode = "404", description = "Задание не найдено")
    })
    @GetMapping("/assignments/{assignmentId}/submissions")
    public List<SubmissionDto> getSubmissions(@PathVariable UUID assignmentId,
                                              @CurrentUser UserEntity currentUser) {
        return submissionService.getSubmissions(assignmentId, currentUser.getId());
    }

    @Operation(summary = "Мой ответ", description = "Студент просматривает свой ответ")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация"),
            @ApiResponse(responseCode = "404", description = "Ответ не найден")
    })
    @GetMapping("/assignments/{assignmentId}/submissions/my")
    public SubmissionDto getMySubmission(@PathVariable UUID assignmentId,
                                         @CurrentUser UserEntity currentUser) {
        return submissionService.getMySubmission(assignmentId, currentUser.getId());
    }

    @Operation(summary = "Отменить отправку", description = "Студент отменяет свой ответ (до дедлайна, если не оценён)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Ответ отменён"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация"),
            @ApiResponse(responseCode = "403", description = "Дедлайн прошёл / уже оценено / не STUDENT"),
            @ApiResponse(responseCode = "404", description = "Ответ не найден")
    })
    @DeleteMapping("/assignments/{assignmentId}/submissions/my")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelSubmission(@PathVariable UUID assignmentId,
                                  @CurrentUser UserEntity currentUser) {
        submissionService.cancelSubmission(assignmentId, currentUser.getId());
    }

    @Operation(summary = "Оценить ответ", description = "Выставление оценки 0-100 (OWNER/TEACHER)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Оценка выставлена"),
            @ApiResponse(responseCode = "400", description = "Невалидная оценка"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация"),
            @ApiResponse(responseCode = "403", description = "Только OWNER/TEACHER"),
            @ApiResponse(responseCode = "404", description = "Ответ не найден")
    })
    @PutMapping("/submissions/{submissionId}/grade")
    public SubmissionDto grade(@PathVariable UUID submissionId,
                               @Valid @RequestBody GradeRequest request,
                               @CurrentUser UserEntity currentUser) {
        return submissionService.grade(submissionId, request, currentUser.getId());
    }
}

package com.example.lms.controller;

import com.example.lms.dto.AddCommentRequest;
import com.example.lms.dto.CommentDto;
import com.example.lms.entity.UserEntity;
import com.example.lms.security.CurrentUser;
import com.example.lms.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Комментарии", description = "Комментарии к заданиям")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "Список комментариев", description = "Комментарии к заданию, отсортированы по createdAt ASC")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация"),
            @ApiResponse(responseCode = "403", description = "Нет доступа к заданию"),
            @ApiResponse(responseCode = "404", description = "Задание не найдено")
    })
    @GetMapping("/assignments/{assignmentId}/comments")
    public Page<CommentDto> getComments(@PathVariable UUID assignmentId,
                                        @CurrentUser UserEntity currentUser,
                                        Pageable pageable) {
        return commentService.getComments(assignmentId, currentUser.getId(), pageable);
    }

    @Operation(summary = "Добавить комментарий", description = "Добавление комментария к заданию")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Комментарий добавлен"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация"),
            @ApiResponse(responseCode = "403", description = "Нет доступа к заданию"),
            @ApiResponse(responseCode = "404", description = "Задание не найдено")
    })
    @PostMapping("/assignments/{assignmentId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto addComment(@PathVariable UUID assignmentId,
                                 @Valid @RequestBody AddCommentRequest request,
                                 @CurrentUser UserEntity currentUser) {
        return commentService.addComment(assignmentId, request, currentUser);
    }
}

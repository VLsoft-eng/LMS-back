package com.example.lms.controller;

import com.example.lms.dto.CreateQuickAssignmentRequest;
import com.example.lms.dto.QuickAssignmentDto;
import com.example.lms.entity.UserEntity;
import com.example.lms.security.CurrentUser;
import com.example.lms.service.QuickAssignmentService;
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
 * TICKET-BE-30: REST-эндпоинт быстрых заданий.
 */
@Tag(name = "Быстрые задания", description = "Создание упрощённых заданий на уроке")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class QuickAssignmentController {

    private final QuickAssignmentService quickAssignmentService;

    @Operation(summary = "Создать быстрое задание")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Задание создано"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "403", description = "Только OWNER/TEACHER")
    })
    @PostMapping("/classes/{classId}/quick-assignments")
    @ResponseStatus(HttpStatus.CREATED)
    public QuickAssignmentDto createQuickAssignment(@PathVariable UUID classId,
                                                     @Valid @RequestBody CreateQuickAssignmentRequest request,
                                                     @CurrentUser UserEntity currentUser) {
        return quickAssignmentService.createQuickAssignment(classId, request, currentUser);
    }
}

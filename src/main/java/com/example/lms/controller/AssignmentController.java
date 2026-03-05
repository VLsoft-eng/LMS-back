package com.example.lms.controller;

import com.example.lms.dto.AssignmentDetailDto;
import com.example.lms.dto.AssignmentDto;
import com.example.lms.dto.CreateAssignmentRequest;
import com.example.lms.entity.UserEntity;
import com.example.lms.security.CurrentUser;
import com.example.lms.service.AssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    @GetMapping("/classes/{classId}/assignments")
    public Page<AssignmentDto> getAssignments(@PathVariable UUID classId,
                                              @CurrentUser UserEntity currentUser,
                                              Pageable pageable) {
        return assignmentService.getAssignments(classId, currentUser.getId(), pageable);
    }

    @PostMapping("/classes/{classId}/assignments")
    @ResponseStatus(HttpStatus.CREATED)
    public AssignmentDto createAssignment(@PathVariable UUID classId,
                                         @Valid @RequestBody CreateAssignmentRequest request,
                                         @CurrentUser UserEntity currentUser) {
        return assignmentService.createAssignment(classId, request, currentUser);
    }

    @GetMapping("/assignments/{id}")
    public AssignmentDetailDto getAssignment(@PathVariable("id") UUID assignmentId,
                                            @CurrentUser UserEntity currentUser) {
        return assignmentService.getAssignment(assignmentId, currentUser.getId());
    }
}

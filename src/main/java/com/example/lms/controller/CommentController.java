package com.example.lms.controller;

import com.example.lms.dto.AddCommentRequest;
import com.example.lms.dto.CommentDto;
import com.example.lms.entity.UserEntity;
import com.example.lms.security.CurrentUser;
import com.example.lms.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/assignments/{assignmentId}/comments")
    public List<CommentDto> getComments(@PathVariable UUID assignmentId,
                                        @CurrentUser UserEntity currentUser) {
        return commentService.getComments(assignmentId, currentUser.getId());
    }

    @PostMapping("/assignments/{assignmentId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto addComment(@PathVariable UUID assignmentId,
                                 @Valid @RequestBody AddCommentRequest request,
                                 @CurrentUser UserEntity currentUser) {
        return commentService.addComment(assignmentId, request, currentUser);
    }
}

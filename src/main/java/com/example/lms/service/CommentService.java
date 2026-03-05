package com.example.lms.service;

import com.example.lms.dto.AddCommentRequest;
import com.example.lms.dto.CommentDto;
import com.example.lms.entity.UserEntity;
import com.example.lms.repository.AssignmentRepository;
import com.example.lms.repository.CommentRepository;
import com.example.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository     commentRepository;
    private final AssignmentRepository  assignmentRepository;
    private final UserRepository        userRepository;
    private final ClassSecurityService  classSecurityService;

    public List<CommentDto> getComments(UUID assignmentId, UUID currentUserId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public CommentDto addComment(UUID assignmentId, AddCommentRequest request, UserEntity currentUser) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}

package com.example.lms.service;

import com.example.lms.dto.AddCommentRequest;
import com.example.lms.dto.CommentDto;
import com.example.lms.entity.AssignmentEntity;
import com.example.lms.entity.CommentEntity;
import com.example.lms.entity.UserEntity;
import com.example.lms.exception.ResourceNotFoundException;
import com.example.lms.repository.AssignmentRepository;
import com.example.lms.repository.CommentRepository;
import com.example.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository     commentRepository;
    private final AssignmentRepository  assignmentRepository;
    private final UserRepository        userRepository;
    private final ClassSecurityService  classSecurityService;

    @Transactional(readOnly = true)
    public Page<CommentDto> getComments(UUID assignmentId, UUID currentUserId, Pageable pageable) {
        AssignmentEntity assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentId));

        classSecurityService.requireMember(assignment.getClassId(), currentUserId);

        return commentRepository.findAllByAssignmentIdOrderByCreatedAtAsc(assignmentId, pageable)
                .map(this::toCommentDto);
    }

    @Transactional
    public CommentDto addComment(UUID assignmentId, AddCommentRequest request, UserEntity currentUser) {
        AssignmentEntity assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentId));

        classSecurityService.requireMember(assignment.getClassId(), currentUser.getId());

        CommentEntity entity = CommentEntity.builder()
                .assignmentId(assignmentId)
                .authorId(currentUser.getId())
                .text(request.text())
                .build();
        entity = commentRepository.save(entity);

        return new CommentDto(
                entity.getId(),
                entity.getAssignmentId(),
                entity.getAuthorId(),
                currentUser.getFirstName() + " " + currentUser.getLastName(),
                currentUser.getAvatarUrl(),
                entity.getText(),
                entity.getCreatedAt()
        );
    }

    private CommentDto toCommentDto(CommentEntity comment) {
        UserEntity author = userRepository.findById(comment.getAuthorId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + comment.getAuthorId()));
        return new CommentDto(
                comment.getId(),
                comment.getAssignmentId(),
                comment.getAuthorId(),
                author.getFirstName() + " " + author.getLastName(),
                author.getAvatarUrl(),
                comment.getText(),
                comment.getCreatedAt()
        );
    }
}

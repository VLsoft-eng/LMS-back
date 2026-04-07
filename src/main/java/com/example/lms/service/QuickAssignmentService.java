package com.example.lms.service;

import com.example.lms.dto.CreateQuickAssignmentRequest;
import com.example.lms.dto.QuickAssignmentDto;
import com.example.lms.dto.TeamShortDto;
import com.example.lms.entity.AssignmentEntity;
import com.example.lms.entity.TeamEntity;
import com.example.lms.entity.UserEntity;
import com.example.lms.exception.ResourceNotFoundException;
import com.example.lms.repository.AssignmentRepository;
import com.example.lms.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * TICKET-BE-27: создание быстрых заданий.
 */
@Service
@RequiredArgsConstructor
public class QuickAssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final TeamRepository teamRepository;
    private final ClassSecurityService classSecurityService;

    @Transactional
    public QuickAssignmentDto createQuickAssignment(UUID classId, CreateQuickAssignmentRequest request,
                                                     UserEntity currentUser) {
        classSecurityService.requireOwnerOrTeacher(classId, currentUser.getId());

        List<TeamEntity> teams = new ArrayList<>();
        if (request.isTeamBased() && request.teamIds() != null && !request.teamIds().isEmpty()) {
            for (UUID teamId : request.teamIds()) {
                TeamEntity team = teamRepository.findById(teamId)
                        .orElseThrow(() -> new ResourceNotFoundException("Team not found: " + teamId));
                if (!team.getClassId().equals(classId)) {
                    throw new IllegalArgumentException("Team " + teamId + " does not belong to class " + classId);
                }
                teams.add(team);
            }
        }

        AssignmentEntity assignment = AssignmentEntity.builder()
                .classId(classId)
                .title(request.title())
                .description(null)
                .type("QUICK")
                .isTeamBased(request.isTeamBased())
                .createdBy(currentUser.getId())
                .deadline(null)
                .filePaths(new ArrayList<>())
                .build();
        assignment = assignmentRepository.save(assignment);

        List<TeamShortDto> teamDtos = teams.stream()
                .map(t -> new TeamShortDto(t.getId(), t.getName()))
                .toList();

        return new QuickAssignmentDto(
                assignment.getId(), assignment.getClassId(), assignment.getTitle(),
                assignment.getType(), assignment.isTeamBased(), teamDtos,
                assignment.getCreatedBy(), assignment.getCreatedAt()
        );
    }
}

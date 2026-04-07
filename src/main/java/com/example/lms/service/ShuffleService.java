package com.example.lms.service;

import com.example.lms.dto.ShuffleRequest;
import com.example.lms.dto.ShuffleResponse;
import com.example.lms.dto.TeamDto;
import com.example.lms.entity.*;
import com.example.lms.exception.ConflictException;
import com.example.lms.repository.ClassMemberRepository;
import com.example.lms.repository.TeamMemberRepository;
import com.example.lms.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * TICKET-BE-25: логика автоматического распределения студентов по командам.
 */
@Service
@RequiredArgsConstructor
public class ShuffleService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ClassMemberRepository classMemberRepository;
    private final ClassSecurityService classSecurityService;
    private final TeamService teamService;

    @Transactional
    public ShuffleResponse shuffle(UUID classId, ShuffleRequest request, UserEntity currentUser) {
        classSecurityService.requireOwnerOrTeacher(classId, currentUser.getId());

        if (request.assignmentId() == null) {
            List<TeamEntity> existing = teamRepository.findAllByClassIdAndAssignmentIdIsNull(classId);
            if (!existing.isEmpty()) {
                throw new ConflictException("Permanent teams already exist for this class. Delete them first.");
            }
        }

        List<ClassMemberEntity> students = classMemberRepository.findAllByClassId(classId).stream()
                .filter(m -> m.getRole() == Role.STUDENT)
                .toList();

        if (request.teamCount() < 2) {
            throw new IllegalArgumentException("teamCount must be >= 2");
        }
        if (request.teamCount() > students.size()) {
            throw new IllegalArgumentException("teamCount cannot exceed number of students (" + students.size() + ")");
        }

        List<UUID> studentIds = new ArrayList<>(students.stream().map(ClassMemberEntity::getUserId).toList());
        Collections.shuffle(studentIds);

        int teamCount = request.teamCount();
        List<List<UUID>> groups = new ArrayList<>();
        for (int i = 0; i < teamCount; i++) {
            groups.add(new ArrayList<>());
        }
        for (int i = 0; i < studentIds.size(); i++) {
            groups.get(i % teamCount).add(studentIds.get(i));
        }

        List<TeamDto> teamDtos = new ArrayList<>();
        for (int i = 0; i < teamCount; i++) {
            List<UUID> group = groups.get(i);

            TeamEntity team = TeamEntity.builder()
                    .classId(classId)
                    .assignmentId(request.assignmentId())
                    .name("Команда " + (i + 1))
                    .createdBy(currentUser.getId())
                    .build();
            team = teamRepository.save(team);

            List<TeamMemberEntity> members = new ArrayList<>();
            for (int j = 0; j < group.size(); j++) {
                TeamMemberEntity member = TeamMemberEntity.builder()
                        .teamId(team.getId())
                        .userId(group.get(j))
                        .isLeader(j == 0)
                        .build();
                members.add(teamMemberRepository.save(member));
            }

            teamDtos.add(teamService.toTeamDto(team, members));
        }

        return new ShuffleResponse(teamDtos, students.size(), students.size() / teamCount);
    }
}

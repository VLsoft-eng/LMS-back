package com.example.lms.service;

import com.example.lms.dto.*;
import com.example.lms.entity.*;
import com.example.lms.exception.ConflictException;
import com.example.lms.exception.ForbiddenException;
import com.example.lms.exception.ResourceNotFoundException;
import com.example.lms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * TICKET-BE-24: бизнес-логика команд и управления составом.
 */
@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ClassSecurityService classSecurityService;
    private final ClassMemberRepository classMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public TeamDto createTeam(UUID classId, CreateTeamRequest request, UserEntity currentUser) {
        classSecurityService.requireOwnerOrTeacher(classId, currentUser.getId());

        validateMembersAreStudents(classId, request.memberUserIds());

        TeamEntity team = TeamEntity.builder()
                .classId(classId)
                .assignmentId(request.assignmentId())
                .name(request.name())
                .createdBy(currentUser.getId())
                .build();
        team = teamRepository.save(team);

        List<UUID> memberIds = new ArrayList<>(request.memberUserIds());
        UUID effectiveLeaderId = request.leaderUserId() != null
                ? request.leaderUserId()
                : memberIds.isEmpty() ? null : memberIds.get(new Random().nextInt(memberIds.size()));

        List<TeamMemberEntity> members = new ArrayList<>();
        for (UUID userId : memberIds) {
            TeamMemberEntity member = TeamMemberEntity.builder()
                    .teamId(team.getId())
                    .userId(userId)
                    .isLeader(userId.equals(effectiveLeaderId))
                    .build();
            members.add(teamMemberRepository.save(member));
        }

        return toTeamDto(team, members);
    }

    @Transactional(readOnly = true)
    public Page<TeamListItemDto> getTeams(UUID classId, UUID assignmentId, UUID currentUserId, Pageable pageable) {
        classSecurityService.requireMember(classId, currentUserId);

        Page<TeamEntity> teams;
        if (assignmentId != null) {
            teams = teamRepository.findAllByClassIdAndAssignmentId(classId, assignmentId, pageable);
        } else {
            teams = teamRepository.findAllByClassId(classId, pageable);
        }

        List<UUID> teamIds = teams.getContent().stream().map(TeamEntity::getId).toList();
        Map<UUID, List<TeamMemberEntity>> membersMap = teamMemberRepository.findAllByTeamIdIn(teamIds)
                .stream().collect(Collectors.groupingBy(TeamMemberEntity::getTeamId));

        Set<UUID> userIds = membersMap.values().stream()
                .flatMap(Collection::stream)
                .map(TeamMemberEntity::getUserId)
                .collect(Collectors.toSet());
        Map<UUID, UserEntity> usersMap = userRepository.findAllById(userIds)
                .stream().collect(Collectors.toMap(UserEntity::getId, u -> u));

        return teams.map(team -> {
            List<TeamMemberEntity> teamMembers = membersMap.getOrDefault(team.getId(), List.of());
            int memberCount = teamMembers.size();
            String leaderName = teamMembers.stream()
                    .filter(TeamMemberEntity::isLeader)
                    .findFirst()
                    .map(m -> {
                        UserEntity u = usersMap.get(m.getUserId());
                        return u != null ? u.getFirstName() + " " + u.getLastName() : null;
                    })
                    .orElse(null);

            return new TeamListItemDto(
                    team.getId(), team.getName(), team.getAssignmentId(),
                    memberCount, leaderName, team.getCreatedAt()
            );
        });
    }

    @Transactional(readOnly = true)
    public TeamDto getTeam(UUID classId, UUID teamId, UUID currentUserId) {
        classSecurityService.requireMember(classId, currentUserId);

        TeamEntity team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found: " + teamId));

        if (!team.getClassId().equals(classId)) {
            throw new ResourceNotFoundException("Team not found in class: " + classId);
        }

        List<TeamMemberEntity> members = teamMemberRepository.findAllByTeamId(teamId);
        return toTeamDto(team, members);
    }

    @Transactional
    public TeamDto updateTeam(UUID classId, UUID teamId, UpdateTeamRequest request, UUID currentUserId) {
        classSecurityService.requireOwnerOrTeacher(classId, currentUserId);

        TeamEntity team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found: " + teamId));

        if (!team.getClassId().equals(classId)) {
            throw new ResourceNotFoundException("Team not found in class: " + classId);
        }

        if (request.name() != null && !request.name().isBlank()) {
            team.setName(request.name());
        }

        if (request.leaderUserId() != null) {
            List<TeamMemberEntity> members = teamMemberRepository.findAllByTeamId(teamId);
            for (TeamMemberEntity member : members) {
                member.setLeader(member.getUserId().equals(request.leaderUserId()));
                teamMemberRepository.save(member);
            }
        }

        team = teamRepository.save(team);
        List<TeamMemberEntity> members = teamMemberRepository.findAllByTeamId(teamId);
        return toTeamDto(team, members);
    }

    @Transactional
    public void deleteTeam(UUID classId, UUID teamId, UUID currentUserId) {
        classSecurityService.requireOwnerOrTeacher(classId, currentUserId);

        TeamEntity team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found: " + teamId));

        if (!team.getClassId().equals(classId)) {
            throw new ResourceNotFoundException("Team not found in class: " + classId);
        }

        teamMemberRepository.deleteAllByTeamId(teamId);
        teamRepository.delete(team);
    }

    @Transactional
    public TeamDto addMember(UUID classId, UUID teamId, AddTeamMemberRequest request, UUID currentUserId) {
        classSecurityService.requireOwnerOrTeacher(classId, currentUserId);

        TeamEntity team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found: " + teamId));

        if (!team.getClassId().equals(classId)) {
            throw new ResourceNotFoundException("Team not found in class: " + classId);
        }

        validateMembersAreStudents(classId, List.of(request.userId()));

        teamMemberRepository.findByTeamIdAndUserId(teamId, request.userId())
                .ifPresent(m -> { throw new ConflictException("User already in team"); });

        TeamMemberEntity member = TeamMemberEntity.builder()
                .teamId(teamId)
                .userId(request.userId())
                .isLeader(request.isLeader())
                .build();
        teamMemberRepository.save(member);

        List<TeamMemberEntity> members = teamMemberRepository.findAllByTeamId(teamId);
        return toTeamDto(team, members);
    }

    @Transactional
    public void removeMember(UUID classId, UUID teamId, UUID userId, UUID currentUserId) {
        classSecurityService.requireOwnerOrTeacher(classId, currentUserId);

        TeamEntity team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found: " + teamId));

        if (!team.getClassId().equals(classId)) {
            throw new ResourceNotFoundException("Team not found in class: " + classId);
        }

        TeamMemberEntity member = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in team"));

        teamMemberRepository.delete(member);
    }

    @Transactional(readOnly = true)
    public List<TeamDto> getMyTeams(UUID classId, UUID currentUserId) {
        classSecurityService.requireMember(classId, currentUserId);

        List<TeamMemberEntity> myMemberships = teamMemberRepository.findAllByUserId(currentUserId);

        List<UUID> teamIds = myMemberships.stream().map(TeamMemberEntity::getTeamId).toList();
        List<TeamEntity> teams = teamRepository.findAllById(teamIds).stream()
                .filter(t -> t.getClassId().equals(classId))
                .toList();

        Map<UUID, List<TeamMemberEntity>> membersMap = teamMemberRepository.findAllByTeamIdIn(
                teams.stream().map(TeamEntity::getId).toList()
        ).stream().collect(Collectors.groupingBy(TeamMemberEntity::getTeamId));

        return teams.stream()
                .map(t -> toTeamDto(t, membersMap.getOrDefault(t.getId(), List.of())))
                .toList();
    }

    private void validateMembersAreStudents(UUID classId, List<UUID> userIds) {
        for (UUID userId : userIds) {
            ClassMemberEntity cm = classMemberRepository.findByClassIdAndUserId(classId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found in class: " + userId));
            if (cm.getRole() != Role.STUDENT) {
                throw new IllegalArgumentException("User " + userId + " is not a STUDENT in this class");
            }
        }
    }

    TeamDto toTeamDto(TeamEntity team, List<TeamMemberEntity> members) {
        Set<UUID> userIds = members.stream().map(TeamMemberEntity::getUserId).collect(Collectors.toSet());
        Map<UUID, UserEntity> usersMap = userRepository.findAllById(userIds)
                .stream().collect(Collectors.toMap(UserEntity::getId, u -> u));

        List<TeamMemberDto> memberDtos = members.stream().map(m -> {
            UserEntity u = usersMap.get(m.getUserId());
            return new TeamMemberDto(
                    m.getUserId(),
                    u != null ? u.getFirstName() : null,
                    u != null ? u.getLastName() : null,
                    m.isLeader(),
                    m.getJoinedAt()
            );
        }).toList();

        return new TeamDto(
                team.getId(), team.getClassId(), team.getAssignmentId(),
                team.getName(), memberDtos, team.getCreatedBy(), team.getCreatedAt()
        );
    }
}

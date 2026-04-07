package com.example.lms.service;

import com.example.lms.dto.*;
import com.example.lms.entity.*;
import com.example.lms.exception.ConflictException;
import com.example.lms.exception.ResourceNotFoundException;
import com.example.lms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * TICKET-BE-26: бизнес-логика групповых оценок и индивидуальных корректировок.
 */
@Service
@RequiredArgsConstructor
public class TeamGradeService {

    private final TeamGradeRepository teamGradeRepository;
    private final IndividualGradeAdjustmentRepository adjustmentRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final AssignmentRepository assignmentRepository;
    private final ClassSecurityService classSecurityService;
    private final UserRepository userRepository;

    @Transactional
    public TeamGradeDto createTeamGrade(UUID assignmentId, CreateTeamGradeRequest request, UserEntity currentUser) {
        AssignmentEntity assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentId));

        classSecurityService.requireOwnerOrTeacher(assignment.getClassId(), currentUser.getId());

        TeamEntity team = teamRepository.findById(request.teamId())
                .orElseThrow(() -> new ResourceNotFoundException("Team not found: " + request.teamId()));

        teamGradeRepository.findByTeamIdAndAssignmentId(request.teamId(), assignmentId)
                .ifPresent(g -> { throw new ConflictException("Team already graded for this assignment"); });

        TeamGradeEntity grade = TeamGradeEntity.builder()
                .teamId(request.teamId())
                .assignmentId(assignmentId)
                .grade(request.grade().shortValue())
                .comment(request.comment())
                .gradedBy(currentUser.getId())
                .build();
        grade = teamGradeRepository.save(grade);

        List<TeamMemberEntity> members = teamMemberRepository.findAllByTeamId(request.teamId());
        List<IndividualGradeAdjustmentEntity> adjustments = new ArrayList<>();
        for (TeamMemberEntity member : members) {
            IndividualGradeAdjustmentEntity adj = IndividualGradeAdjustmentEntity.builder()
                    .teamGradeId(grade.getId())
                    .studentId(member.getUserId())
                    .adjustment((short) 0)
                    .finalGrade(request.grade().shortValue())
                    .gradedBy(currentUser.getId())
                    .build();
            adjustments.add(adjustmentRepository.save(adj));
        }

        return toTeamGradeDto(grade, team.getName(), adjustments);
    }

    @Transactional(readOnly = true)
    public Page<TeamGradeListItemDto> getTeamGrades(UUID assignmentId, UUID currentUserId, Pageable pageable) {
        AssignmentEntity assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentId));

        classSecurityService.requireMember(assignment.getClassId(), currentUserId);

        Page<TeamGradeEntity> grades = teamGradeRepository.findAllByAssignmentId(assignmentId, pageable);

        Set<UUID> teamIds = grades.getContent().stream().map(TeamGradeEntity::getTeamId).collect(Collectors.toSet());
        Map<UUID, TeamEntity> teamsMap = teamRepository.findAllById(teamIds)
                .stream().collect(Collectors.toMap(TeamEntity::getId, t -> t));

        return grades.map(g -> {
            TeamEntity team = teamsMap.get(g.getTeamId());
            int memberCount = teamMemberRepository.countByTeamId(g.getTeamId());
            return new TeamGradeListItemDto(
                    g.getId(), g.getTeamId(),
                    team != null ? team.getName() : null,
                    g.getGrade(), memberCount, g.getGradedAt()
            );
        });
    }

    @Transactional
    public TeamGradeDto updateTeamGrade(UUID assignmentId, UUID teamGradeId,
                                         UpdateTeamGradeRequest request, UserEntity currentUser) {
        AssignmentEntity assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentId));

        classSecurityService.requireOwnerOrTeacher(assignment.getClassId(), currentUser.getId());

        TeamGradeEntity grade = teamGradeRepository.findById(teamGradeId)
                .orElseThrow(() -> new ResourceNotFoundException("Team grade not found: " + teamGradeId));

        grade.setGrade(request.grade().shortValue());
        grade.setComment(request.comment());
        grade = teamGradeRepository.save(grade);

        List<IndividualGradeAdjustmentEntity> adjustments = adjustmentRepository.findAllByTeamGradeId(teamGradeId);
        for (IndividualGradeAdjustmentEntity adj : adjustments) {
            adj.setFinalGrade(clamp(request.grade() + adj.getAdjustment()));
            adj.setGradedAt(Instant.now());
            adjustmentRepository.save(adj);
        }

        TeamEntity team = teamRepository.findById(grade.getTeamId())
                .orElseThrow(() -> new ResourceNotFoundException("Team not found: " + grade.getTeamId()));

        adjustments = adjustmentRepository.findAllByTeamGradeId(teamGradeId);
        return toTeamGradeDto(grade, team.getName(), adjustments);
    }

    @Transactional
    public IndividualAdjustmentDto updateAdjustment(UUID assignmentId, UUID teamGradeId, UUID studentId,
                                                     UpdateAdjustmentRequest request, UserEntity currentUser) {
        AssignmentEntity assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentId));

        classSecurityService.requireOwnerOrTeacher(assignment.getClassId(), currentUser.getId());

        TeamGradeEntity grade = teamGradeRepository.findById(teamGradeId)
                .orElseThrow(() -> new ResourceNotFoundException("Team grade not found: " + teamGradeId));

        IndividualGradeAdjustmentEntity adj = adjustmentRepository.findByTeamGradeIdAndStudentId(teamGradeId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Adjustment not found for student: " + studentId));

        adj.setAdjustment(request.adjustment().shortValue());
        adj.setFinalGrade(clamp(grade.getGrade() + request.adjustment()));
        adj.setComment(request.comment());
        adj.setGradedBy(currentUser.getId());
        adj.setGradedAt(Instant.now());
        adj = adjustmentRepository.save(adj);

        UserEntity student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + studentId));

        return new IndividualAdjustmentDto(
                adj.getStudentId(),
                student.getFirstName() + " " + student.getLastName(),
                grade.getGrade(), adj.getAdjustment(), adj.getFinalGrade(),
                adj.getComment(), adj.getGradedBy(), adj.getGradedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<IndividualAdjustmentDto> getAdjustments(UUID assignmentId, UUID teamGradeId, UUID currentUserId) {
        AssignmentEntity assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentId));

        classSecurityService.requireMember(assignment.getClassId(), currentUserId);

        TeamGradeEntity grade = teamGradeRepository.findById(teamGradeId)
                .orElseThrow(() -> new ResourceNotFoundException("Team grade not found: " + teamGradeId));

        List<IndividualGradeAdjustmentEntity> adjustments = adjustmentRepository.findAllByTeamGradeId(teamGradeId);

        Set<UUID> studentIds = adjustments.stream().map(IndividualGradeAdjustmentEntity::getStudentId).collect(Collectors.toSet());
        Map<UUID, UserEntity> usersMap = userRepository.findAllById(studentIds)
                .stream().collect(Collectors.toMap(UserEntity::getId, u -> u));

        return adjustments.stream().map(adj -> {
            UserEntity student = usersMap.get(adj.getStudentId());
            return new IndividualAdjustmentDto(
                    adj.getStudentId(),
                    student != null ? student.getFirstName() + " " + student.getLastName() : null,
                    grade.getGrade(), adj.getAdjustment(), adj.getFinalGrade(),
                    adj.getComment(), adj.getGradedBy(), adj.getGradedAt()
            );
        }).toList();
    }

    @Transactional(readOnly = true)
    public MyTeamGradeDto getMyTeamGrade(UUID assignmentId, UUID currentUserId) {
        List<TeamMemberEntity> memberships = teamMemberRepository.findAllByUserId(currentUserId);

        for (TeamMemberEntity membership : memberships) {
            Optional<TeamGradeEntity> gradeOpt = teamGradeRepository
                    .findByTeamIdAndAssignmentId(membership.getTeamId(), assignmentId);

            if (gradeOpt.isPresent()) {
                TeamGradeEntity grade = gradeOpt.get();
                TeamEntity team = teamRepository.findById(grade.getTeamId())
                        .orElseThrow(() -> new ResourceNotFoundException("Team not found"));

                IndividualGradeAdjustmentEntity adj = adjustmentRepository
                        .findByTeamGradeIdAndStudentId(grade.getId(), currentUserId)
                        .orElseThrow(() -> new ResourceNotFoundException("Adjustment not found"));

                return new MyTeamGradeDto(
                        team.getId(), team.getName(),
                        grade.getGrade(), adj.getAdjustment(), adj.getFinalGrade(),
                        adj.getComment(), grade.getGradedAt()
                );
            }
        }

        throw new ResourceNotFoundException("Team grade not found for this assignment");
    }

    private TeamGradeDto toTeamGradeDto(TeamGradeEntity grade, String teamName,
                                         List<IndividualGradeAdjustmentEntity> adjustments) {
        Set<UUID> studentIds = adjustments.stream()
                .map(IndividualGradeAdjustmentEntity::getStudentId).collect(Collectors.toSet());
        Map<UUID, UserEntity> usersMap = userRepository.findAllById(studentIds)
                .stream().collect(Collectors.toMap(UserEntity::getId, u -> u));

        List<IndividualAdjustmentDto> adjDtos = adjustments.stream().map(adj -> {
            UserEntity student = usersMap.get(adj.getStudentId());
            return new IndividualAdjustmentDto(
                    adj.getStudentId(),
                    student != null ? student.getFirstName() + " " + student.getLastName() : null,
                    grade.getGrade(), adj.getAdjustment(), adj.getFinalGrade(),
                    adj.getComment(), adj.getGradedBy(), adj.getGradedAt()
            );
        }).toList();

        return new TeamGradeDto(
                grade.getId(), grade.getTeamId(), teamName, grade.getAssignmentId(),
                grade.getGrade(), grade.getComment(), adjDtos,
                grade.getGradedBy(), grade.getGradedAt()
        );
    }

    private short clamp(int value) {
        return (short) Math.max(0, Math.min(100, value));
    }
}

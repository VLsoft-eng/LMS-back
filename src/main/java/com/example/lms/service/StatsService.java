package com.example.lms.service;

import com.example.lms.dto.ClassStatsDto;
import com.example.lms.dto.StudentStatDto;
import com.example.lms.entity.AssignmentEntity;
import com.example.lms.entity.ClassMemberEntity;
import com.example.lms.entity.Role;
import com.example.lms.entity.SubmissionEntity;
import com.example.lms.entity.UserEntity;
import com.example.lms.exception.ResourceNotFoundException;
import com.example.lms.repository.AssignmentRepository;
import com.example.lms.repository.ClassMemberRepository;
import com.example.lms.repository.ClassRepository;
import com.example.lms.repository.SubmissionRepository;
import com.example.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final ClassRepository classRepository;
    private final ClassMemberRepository classMemberRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final ClassSecurityService classSecurityService;

    @Transactional(readOnly = true)
    public ClassStatsDto getClassStats(UUID classId, UUID currentUserId) {
        classSecurityService.requireOwnerOrTeacher(classId, currentUserId);

        var classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + classId));

        List<AssignmentEntity> assignments = assignmentRepository.findAllByClassId(classId);
        List<UUID> assignmentIds = assignments.stream().map(AssignmentEntity::getId).toList();

        List<ClassMemberEntity> allMembers = classMemberRepository.findAllByClassId(classId);
        List<ClassMemberEntity> students = allMembers.stream()
                .filter(m -> m.getRole() == Role.STUDENT)
                .toList();

        List<UUID> studentIds = students.stream().map(ClassMemberEntity::getUserId).toList();

        Map<UUID, UserEntity> userMap = userRepository.findAllById(studentIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, u -> u));

        List<SubmissionEntity> allSubmissions = assignmentIds.isEmpty()
                ? List.of()
                : submissionRepository.findAllByAssignmentIdIn(assignmentIds);

        Map<UUID, List<SubmissionEntity>> submissionsByStudent = allSubmissions.stream()
                .filter(s -> studentIds.contains(s.getStudentId()))
                .collect(Collectors.groupingBy(SubmissionEntity::getStudentId));

        Instant now = Instant.now();
        long passedDeadlineCount = assignments.stream()
                .filter(a -> a.getDeadline() != null && a.getDeadline().isBefore(now))
                .count();

        List<StudentStatDto> studentStats = students.stream()
                .map(member -> buildStudentStat(member, userMap, submissionsByStudent, assignments, now))
                .sorted((a, b) -> {
                    if (a.averageGrade() == null && b.averageGrade() == null) return 0;
                    if (a.averageGrade() == null) return 1;
                    if (b.averageGrade() == null) return -1;
                    return Double.compare(b.averageGrade(), a.averageGrade());
                })
                .toList();

        double classAverageGrade = studentStats.stream()
                .filter(s -> s.averageGrade() != null)
                .mapToDouble(StudentStatDto::averageGrade)
                .average()
                .orElse(Double.NaN);

        long studentsWithSubmissions = submissionsByStudent.keySet().stream()
                .filter(studentIds::contains)
                .count();

        double submissionRate = students.isEmpty() ? 0.0
                : (double) studentsWithSubmissions / students.size() * 100.0;

        return new ClassStatsDto(
                classId,
                classEntity.getName(),
                assignments.size(),
                students.size(),
                Double.isNaN(classAverageGrade) ? null : Math.round(classAverageGrade * 10.0) / 10.0,
                Math.round(submissionRate * 10.0) / 10.0,
                studentStats
        );
    }

    private StudentStatDto buildStudentStat(
            ClassMemberEntity member,
            Map<UUID, UserEntity> userMap,
            Map<UUID, List<SubmissionEntity>> submissionsByStudent,
            List<AssignmentEntity> assignments,
            Instant now
    ) {
        UUID studentId = member.getUserId();
        UserEntity user = userMap.get(studentId);
        String name = user != null ? user.getFirstName() + " " + user.getLastName() : "Неизвестный";
        String avatarUrl = user != null ? user.getAvatarUrl() : null;

        List<SubmissionEntity> subs = submissionsByStudent.getOrDefault(studentId, List.of());

        int submittedCount = subs.size();

        List<SubmissionEntity> graded = subs.stream()
                .filter(s -> s.getGrade() != null)
                .toList();
        int gradedCount = graded.size();

        Double averageGrade = graded.isEmpty() ? null
                : Math.round(graded.stream().mapToInt(s -> s.getGrade()).average().orElse(0) * 10.0) / 10.0;

        Set<UUID> submittedAssignmentIds = subs.stream()
                .map(SubmissionEntity::getAssignmentId)
                .collect(Collectors.toSet());

        int missedCount = (int) assignments.stream()
                .filter(a -> a.getDeadline() != null && a.getDeadline().isBefore(now))
                .filter(a -> !submittedAssignmentIds.contains(a.getId()))
                .count();

        return new StudentStatDto(studentId, name, avatarUrl, submittedCount, gradedCount, averageGrade, missedCount);
    }
}

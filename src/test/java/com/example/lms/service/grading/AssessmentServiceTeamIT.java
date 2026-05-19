package com.example.lms.service.grading;

import com.example.lms.dto.grading.CreateAssessmentRequest;
import com.example.lms.dto.grading.CriterionScoreInput;
import com.example.lms.dto.grading.UpdateAssessmentRequest;
import com.example.lms.entity.AssignmentEntity;
import com.example.lms.entity.ClassEntity;
import com.example.lms.entity.ClassMemberEntity;
import com.example.lms.entity.IndividualGradeAdjustmentEntity;
import com.example.lms.entity.Role;
import com.example.lms.entity.TeamEntity;
import com.example.lms.entity.TeamGradeEntity;
import com.example.lms.entity.TeamMemberEntity;
import com.example.lms.entity.UserEntity;
import com.example.lms.entity.grading.AssessmentEntity;
import com.example.lms.entity.grading.CriterionEntity;
import com.example.lms.entity.grading.CriterionKind;
import com.example.lms.entity.grading.CriterionRole;
import com.example.lms.entity.grading.RubricEntity;
import com.example.lms.exception.RubricConflictException;
import com.example.lms.repository.AssessmentRepository;
import com.example.lms.repository.AssignmentRepository;
import com.example.lms.repository.ClassMemberRepository;
import com.example.lms.repository.ClassRepository;
import com.example.lms.repository.IndividualGradeAdjustmentRepository;
import com.example.lms.repository.RepositoryTestContextInitializer;
import com.example.lms.repository.RubricRepository;
import com.example.lms.repository.TeamGradeRepository;
import com.example.lms.repository.TeamMemberRepository;
import com.example.lms.repository.TeamRepository;
import com.example.lms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.EnabledIf;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TICKET-BE-43: end-to-end проверка командного flow ассессмента через teamId.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@ContextConfiguration(initializers = RepositoryTestContextInitializer.class)
@EnabledIf(
        expression = "#{T(com.example.lms.DockerAvailability).isAvailable()}",
        loadContext = false,
        reason = "Docker is not available"
)
class AssessmentServiceTeamIT {

    @Autowired AssessmentService assessmentService;
    @Autowired AssessmentRepository assessmentRepository;
    @Autowired RubricRepository rubricRepository;
    @Autowired AssignmentRepository assignmentRepository;
    @Autowired ClassRepository classRepository;
    @Autowired ClassMemberRepository classMemberRepository;
    @Autowired TeamRepository teamRepository;
    @Autowired TeamMemberRepository teamMemberRepository;
    @Autowired TeamGradeRepository teamGradeRepository;
    @Autowired IndividualGradeAdjustmentRepository adjustmentRepository;
    @Autowired UserRepository userRepository;

    private UserEntity teacher;
    private UserEntity student1;
    private UserEntity student2;
    private ClassEntity cls;
    private AssignmentEntity assignment;
    private RubricEntity rubric;
    private TeamEntity team;
    private CriterionEntity boolCriterion;
    private CriterionEntity percentCriterion;
    private static int idx = 0;

    @BeforeEach
    void setup() {
        idx++;
        teacher = save(user("teacher" + idx));
        student1 = save(user("s1-" + idx));
        student2 = save(user("s2-" + idx));

        cls = classRepository.save(ClassEntity.builder()
                .name("Class " + idx)
                .code("CLS" + String.format("%04d", idx))
                .ownerId(teacher.getId())
                .build());
        classMemberRepository.save(member(cls.getId(), teacher.getId(), Role.OWNER));
        classMemberRepository.save(member(cls.getId(), student1.getId(), Role.STUDENT));
        classMemberRepository.save(member(cls.getId(), student2.getId(), Role.STUDENT));

        assignment = assignmentRepository.save(AssignmentEntity.builder()
                .classId(cls.getId())
                .title("Team Lab " + idx)
                .createdBy(teacher.getId())
                .isTeamBased(true)
                .build());

        // Build rubric snapshot directly
        boolCriterion = CriterionEntity.builder()
                .ordinal(0).title("Решено").kind(CriterionKind.BOOLEAN).role(CriterionRole.PRIMARY)
                .maxPoints(new BigDecimal("4.00"))
                .build();
        percentCriterion = CriterionEntity.builder()
                .ordinal(1).title("Качество").kind(CriterionKind.PERCENT).role(CriterionRole.PRIMARY)
                .maxPoints(new BigDecimal("6.00"))
                .build();
        rubric = RubricEntity.builder()
                .assignmentId(assignment.getId())
                .name("Team rubric")
                .totalMaxPoints(new BigDecimal("10.00"))
                .allowOvercap(false)
                .criteria(new ArrayList<>(List.of(boolCriterion, percentCriterion)))
                .build();
        rubric = rubricRepository.save(rubric);
        // refresh criterion ids after persist
        boolCriterion = rubric.getCriteria().get(0);
        percentCriterion = rubric.getCriteria().get(1);

        assignment.setRubricId(rubric.getId());
        assignment = assignmentRepository.save(assignment);

        team = teamRepository.save(TeamEntity.builder()
                .classId(cls.getId())
                .assignmentId(assignment.getId())
                .name("Team A")
                .createdBy(teacher.getId())
                .build());
        teamMemberRepository.save(TeamMemberEntity.builder()
                .teamId(team.getId()).userId(student1.getId()).isLeader(true).build());
        teamMemberRepository.save(TeamMemberEntity.builder()
                .teamId(team.getId()).userId(student2.getId()).isLeader(false).build());
    }

    @Test
    void create_byTeamId_creates_teamGrade_and_adjustments_for_each_member() {
        var dto = assessmentService.create(assignment.getId(), new CreateAssessmentRequest(
                null, team.getId(), null,
                List.of(
                        new CriterionScoreInput(boolCriterion.getId(),    true,  null,                  null, null),
                        new CriterionScoreInput(percentCriterion.getId(), null,  new BigDecimal("50.00"), null, null)
                )
        ), teacher);

        // 4.00 + 3.00 = 7.00 → 70%
        assertThat(dto.primarySum()).isEqualByComparingTo("7.00");
        assertThat(dto.finalScore()).isEqualByComparingTo("7.00");
        assertThat(dto.finalScoreNormalized()).isEqualTo((short) 70);

        TeamGradeEntity tg = teamGradeRepository.findByTeamIdAndAssignmentId(team.getId(), assignment.getId())
                .orElseThrow();
        assertThat(tg.getGrade()).isEqualTo((short) 70);
        assertThat(dto.teamGradeId()).isEqualTo(tg.getId());

        List<IndividualGradeAdjustmentEntity> adjustments = adjustmentRepository.findAllByTeamGradeId(tg.getId());
        assertThat(adjustments).hasSize(2);
        assertThat(adjustments).allSatisfy(adj -> {
            assertThat(adj.getAdjustment()).isEqualTo((short) 0);
            assertThat(adj.getFinalGrade()).isEqualTo((short) 70);
        });
    }

    @Test
    void update_propagates_new_normalized_to_teamGrade_and_adjustments() {
        var created = assessmentService.create(assignment.getId(), new CreateAssessmentRequest(
                null, team.getId(), null,
                List.of(
                        new CriterionScoreInput(boolCriterion.getId(),    true,  null,                  null, null),
                        new CriterionScoreInput(percentCriterion.getId(), null,  new BigDecimal("50.00"), null, null)
                )
        ), teacher);

        // Сместим одного члена команды на +5
        IndividualGradeAdjustmentEntity victor = adjustmentRepository.findByTeamGradeIdAndStudentId(
                created.teamGradeId(), student1.getId()).orElseThrow();
        victor.setAdjustment((short) 5);
        adjustmentRepository.save(victor);

        // Пересчёт: 4 + 6×1 = 10 → 100
        assessmentService.update(created.id(), new UpdateAssessmentRequest(List.of(
                new CriterionScoreInput(boolCriterion.getId(),    true, null,                   null, null),
                new CriterionScoreInput(percentCriterion.getId(), null, new BigDecimal("100.00"), null, null)
        )), teacher);

        TeamGradeEntity tg = teamGradeRepository.findById(created.teamGradeId()).orElseThrow();
        assertThat(tg.getGrade()).isEqualTo((short) 100);

        IndividualGradeAdjustmentEntity victorAfter = adjustmentRepository.findByTeamGradeIdAndStudentId(
                created.teamGradeId(), student1.getId()).orElseThrow();
        // 100 + 5 = 105 → clamp до 100
        assertThat(victorAfter.getFinalGrade()).isEqualTo((short) 100);

        IndividualGradeAdjustmentEntity other = adjustmentRepository.findByTeamGradeIdAndStudentId(
                created.teamGradeId(), student2.getId()).orElseThrow();
        // adjustment был 0 → finalGrade = 100
        assertThat(other.getFinalGrade()).isEqualTo((short) 100);
    }

    @Test
    void delete_removes_teamGrade_and_adjustments() {
        var created = assessmentService.create(assignment.getId(), new CreateAssessmentRequest(
                null, team.getId(), null,
                List.of(
                        new CriterionScoreInput(boolCriterion.getId(),    true, null,                  null, null),
                        new CriterionScoreInput(percentCriterion.getId(), null, new BigDecimal("80.00"), null, null)
                )
        ), teacher);

        UUID teamGradeId = created.teamGradeId();
        assertThat(teamGradeId).isNotNull();
        assertThat(adjustmentRepository.findAllByTeamGradeId(teamGradeId)).hasSize(2);

        assessmentService.delete(created.id(), teacher);

        assertThat(assessmentRepository.findById(created.id())).isEmpty();
        assertThat(teamGradeRepository.findById(teamGradeId)).isEmpty();
        assertThat(adjustmentRepository.findAllByTeamGradeId(teamGradeId)).isEmpty();
    }

    @Test
    void create_rejects_when_both_submissionId_and_teamId_provided() {
        assertThatThrownBy(() -> assessmentService.create(assignment.getId(), new CreateAssessmentRequest(
                UUID.randomUUID(), team.getId(), null,
                List.of(
                        new CriterionScoreInput(boolCriterion.getId(),    true, null, null, null),
                        new CriterionScoreInput(percentCriterion.getId(), null, new BigDecimal("50.00"), null, null)
                )
        ), teacher))
                .isInstanceOf(RubricConflictException.class)
                .satisfies(e -> assertThat(((RubricConflictException) e).getCode())
                        .isEqualTo("ASSESSMENT_TARGET_MUTUALLY_EXCLUSIVE"));
    }

    @Test
    void create_rejects_when_team_belongs_to_another_class() {
        ClassEntity otherClass = classRepository.save(ClassEntity.builder()
                .name("Other " + idx)
                .code("OTH" + String.format("%04d", idx))
                .ownerId(teacher.getId())
                .build());
        TeamEntity foreign = teamRepository.save(TeamEntity.builder()
                .classId(otherClass.getId()).name("Foreign").createdBy(teacher.getId()).build());

        assertThatThrownBy(() -> assessmentService.create(assignment.getId(), new CreateAssessmentRequest(
                null, foreign.getId(), null,
                List.of(
                        new CriterionScoreInput(boolCriterion.getId(),    true, null, null, null),
                        new CriterionScoreInput(percentCriterion.getId(), null, new BigDecimal("50.00"), null, null)
                )
        ), teacher))
                .isInstanceOf(RubricConflictException.class);
    }

    @Test
    void create_twice_for_same_team_returns_already_exists() {
        assessmentService.create(assignment.getId(), new CreateAssessmentRequest(
                null, team.getId(), null,
                List.of(
                        new CriterionScoreInput(boolCriterion.getId(),    true, null, null, null),
                        new CriterionScoreInput(percentCriterion.getId(), null, new BigDecimal("50.00"), null, null)
                )
        ), teacher);

        assertThatThrownBy(() -> assessmentService.create(assignment.getId(), new CreateAssessmentRequest(
                null, team.getId(), null,
                List.of(
                        new CriterionScoreInput(boolCriterion.getId(),    false, null, null, null),
                        new CriterionScoreInput(percentCriterion.getId(), null,  new BigDecimal("0.00"), null, null)
                )
        ), teacher))
                .isInstanceOf(RubricConflictException.class)
                .satisfies(e -> assertThat(((RubricConflictException) e).getCode())
                        .isEqualTo("ASSESSMENT_ALREADY_EXISTS"));
    }

    private <T> T save(T entity) {
        if (entity instanceof UserEntity u) {
            return (T) userRepository.save(u);
        }
        throw new IllegalArgumentException("Use specific repo");
    }

    private UserEntity user(String emailPrefix) {
        return UserEntity.builder()
                .firstName("F")
                .lastName("L")
                .email(emailPrefix + "@test.local")
                .passwordHash("hash")
                .build();
    }

    private ClassMemberEntity member(UUID classId, UUID userId, Role role) {
        return ClassMemberEntity.builder()
                .classId(classId)
                .userId(userId)
                .role(role)
                .build();
    }
}

package com.example.lms.service.grading;

import com.example.lms.dto.grading.CreateAssessmentRequest;
import com.example.lms.dto.grading.CriterionScoreInput;
import com.example.lms.dto.grading.MyAssessmentDto;
import com.example.lms.entity.AssignmentEntity;
import com.example.lms.entity.ClassEntity;
import com.example.lms.entity.ClassMemberEntity;
import com.example.lms.entity.Role;
import com.example.lms.entity.SubmissionEntity;
import com.example.lms.entity.UserEntity;
import com.example.lms.entity.grading.CriterionEntity;
import com.example.lms.entity.grading.CriterionKind;
import com.example.lms.entity.grading.CriterionRole;
import com.example.lms.entity.grading.RubricEntity;
import com.example.lms.repository.AssignmentRepository;
import com.example.lms.repository.ClassMemberRepository;
import com.example.lms.repository.ClassRepository;
import com.example.lms.repository.RepositoryTestContextInitializer;
import com.example.lms.repository.RubricRepository;
import com.example.lms.repository.SubmissionRepository;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Контракт student-facing endpoint: classId должен быть включён в MyAssessmentDto,
 * чтобы FE мог группировать оценки по классам и строить ссылки на класс.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@ContextConfiguration(initializers = RepositoryTestContextInitializer.class)
@EnabledIf(
        expression = "#{T(com.example.lms.DockerAvailability).isAvailable()}",
        loadContext = false,
        reason = "Docker is not available"
)
class AssessmentServiceMyAssessmentsIT {

    @Autowired AssessmentService assessmentService;
    @Autowired AssignmentRepository assignmentRepository;
    @Autowired RubricRepository rubricRepository;
    @Autowired SubmissionRepository submissionRepository;
    @Autowired ClassRepository classRepository;
    @Autowired ClassMemberRepository classMemberRepository;
    @Autowired UserRepository userRepository;

    private UserEntity teacher;
    private UserEntity student;
    private static int idx = 0;

    @BeforeEach
    void setup() {
        idx++;
        teacher = userRepository.save(UserEntity.builder()
                .firstName("T").lastName("L").email("tmy" + idx + "@i.test").passwordHash("h").build());
        student = userRepository.save(UserEntity.builder()
                .firstName("S").lastName("S").email("smy" + idx + "@i.test").passwordHash("h").build());
    }

    @Test
    void my_assessments_include_classId_per_entry() {
        // два класса, в каждом по заданию с рубрикой, студент в обоих классах
        ClassEntity classA = createClassWith(teacher, student, "MYA");
        ClassEntity classB = createClassWith(teacher, student, "MYB");

        var assessmentA = createGradedAssessment(classA, "Лаба A", "10.00");
        var assessmentB = createGradedAssessment(classB, "Лаба B", "5.00");

        List<MyAssessmentDto> my = assessmentService.listMyAssessments(student.getId());

        assertThat(my).hasSize(2);
        assertThat(my).extracting(MyAssessmentDto::classId)
                .containsExactlyInAnyOrder(classA.getId(), classB.getId());
        // sanity: classId согласован с assignmentId
        for (MyAssessmentDto dto : my) {
            AssignmentEntity asg = assignmentRepository.findById(dto.assignmentId()).orElseThrow();
            assertThat(dto.classId()).isEqualTo(asg.getClassId());
        }
    }

    // ─────────── helpers ───────────

    private ClassEntity createClassWith(UserEntity teacher, UserEntity student, String prefix) {
        ClassEntity cls = classRepository.save(ClassEntity.builder()
                .name(prefix + idx)
                .code(prefix + String.format("%04d", idx))
                .ownerId(teacher.getId())
                .build());
        classMemberRepository.save(ClassMemberEntity.builder()
                .classId(cls.getId()).userId(teacher.getId()).role(Role.OWNER).build());
        classMemberRepository.save(ClassMemberEntity.builder()
                .classId(cls.getId()).userId(student.getId()).role(Role.STUDENT).build());
        return cls;
    }

    private com.example.lms.dto.grading.AssessmentDto createGradedAssessment(
            ClassEntity cls, String title, String totalMax) {
        AssignmentEntity assignment = assignmentRepository.save(AssignmentEntity.builder()
                .classId(cls.getId())
                .title(title)
                .createdBy(teacher.getId())
                .isTeamBased(false)
                .build());

        CriterionEntity c = CriterionEntity.builder()
                .ordinal(0).title("Сдано").kind(CriterionKind.BOOLEAN).role(CriterionRole.PRIMARY)
                .maxPoints(new BigDecimal(totalMax)).build();
        RubricEntity rubric = rubricRepository.save(RubricEntity.builder()
                .assignmentId(assignment.getId())
                .name(title + " rubric")
                .totalMaxPoints(new BigDecimal(totalMax))
                .allowOvercap(false)
                .criteria(new ArrayList<>(List.of(c)))
                .build());
        assignment.setRubricId(rubric.getId());
        assignment = assignmentRepository.save(assignment);

        SubmissionEntity sub = submissionRepository.save(SubmissionEntity.builder()
                .assignmentId(assignment.getId())
                .studentId(student.getId())
                .answerText("done")
                .build());

        return assessmentService.create(assignment.getId(), new CreateAssessmentRequest(
                sub.getId(), null, null,
                List.of(new CriterionScoreInput(rubric.getCriteria().get(0).getId(), true, null, null, null))
        ), teacher);
    }
}

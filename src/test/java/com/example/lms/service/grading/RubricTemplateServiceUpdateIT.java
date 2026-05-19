package com.example.lms.service.grading;

import com.example.lms.dto.grading.CreateRubricTemplateRequest;
import com.example.lms.dto.grading.CriterionTemplateInput;
import com.example.lms.dto.grading.RubricTemplateDto;
import com.example.lms.dto.grading.UpdateRubricTemplateRequest;
import com.example.lms.entity.ClassEntity;
import com.example.lms.entity.ClassMemberEntity;
import com.example.lms.entity.Role;
import com.example.lms.entity.UserEntity;
import com.example.lms.entity.grading.CriterionKind;
import com.example.lms.entity.grading.CriterionRole;
import com.example.lms.repository.ClassMemberRepository;
import com.example.lms.repository.ClassRepository;
import com.example.lms.repository.RepositoryTestContextInitializer;
import com.example.lms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.EnabledIf;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Регрессия на баг: PUT /rubric-templates/{id} падал на UNIQUE (rubric_template_id, ordinal),
 * потому что Hibernate с orphanRemoval мог вставить новые criterion_templates до удаления старых.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@ContextConfiguration(initializers = RepositoryTestContextInitializer.class)
@EnabledIf(
        expression = "#{T(com.example.lms.DockerAvailability).isAvailable()}",
        loadContext = false,
        reason = "Docker is not available"
)
class RubricTemplateServiceUpdateIT {

    @Autowired RubricTemplateService rubricTemplateService;
    @Autowired ClassRepository classRepository;
    @Autowired ClassMemberRepository classMemberRepository;
    @Autowired UserRepository userRepository;

    private UserEntity teacher;
    private ClassEntity cls;
    private static int idx = 0;

    @BeforeEach
    void setup() {
        idx++;
        teacher = userRepository.save(UserEntity.builder()
                .firstName("T").lastName("L")
                .email("t" + idx + "@upd.it")
                .passwordHash("h").build());
        cls = classRepository.save(ClassEntity.builder()
                .name("Cls " + idx)
                .code("UPD" + String.format("%04d", idx))
                .ownerId(teacher.getId()).build());
        classMemberRepository.save(ClassMemberEntity.builder()
                .classId(cls.getId()).userId(teacher.getId()).role(Role.OWNER).build());
    }

    @Test
    void update_replaces_criteria_without_unique_violation() {
        RubricTemplateDto created = rubricTemplateService.create(cls.getId(), new CreateRubricTemplateRequest(
                "Rubric v1",
                null,
                new BigDecimal("10.00"),
                false,
                List.of(
                        new CriterionTemplateInput(0, "Old A", null, CriterionKind.BOOLEAN, CriterionRole.PRIMARY,
                                new BigDecimal("4.00"), null, null, null),
                        new CriterionTemplateInput(1, "Old B", null, CriterionKind.PERCENT, CriterionRole.PRIMARY,
                                new BigDecimal("6.00"), null, null, null)
                )
        ), teacher);

        // те же ordinals, что в исходном шаблоне — именно тут раньше падал UNIQUE
        assertThatCode(() -> rubricTemplateService.update(created.id(), new UpdateRubricTemplateRequest(
                "Rubric v2",
                "Перебалансировано",
                new BigDecimal("10.00"),
                false,
                List.of(
                        new CriterionTemplateInput(0, "New A", null, CriterionKind.BOOLEAN, CriterionRole.PRIMARY,
                                new BigDecimal("3.00"), null, null, null),
                        new CriterionTemplateInput(1, "New B", null, CriterionKind.SCORE, CriterionRole.PRIMARY,
                                new BigDecimal("7.00"), null,
                                new BigDecimal("0.00"), new BigDecimal("10.00"))
                )
        ), teacher)).doesNotThrowAnyException();

        RubricTemplateDto reloaded = rubricTemplateService.getById(created.id(), teacher.getId());
        assertThat(reloaded.name()).isEqualTo("Rubric v2");
        assertThat(reloaded.description()).isEqualTo("Перебалансировано");
        assertThat(reloaded.criteria()).hasSize(2);
        assertThat(reloaded.criteria()).extracting("title")
                .containsExactlyInAnyOrder("New A", "New B");
        assertThat(reloaded.criteria()).extracting("ordinal")
                .containsExactlyInAnyOrder(0, 1);
    }

    @Test
    void update_can_change_number_of_criteria() {
        RubricTemplateDto created = rubricTemplateService.create(cls.getId(), new CreateRubricTemplateRequest(
                "Single",
                null,
                new BigDecimal("10.00"),
                false,
                List.of(
                        new CriterionTemplateInput(0, "Only", null, CriterionKind.BOOLEAN, CriterionRole.PRIMARY,
                                new BigDecimal("10.00"), null, null, null)
                )
        ), teacher);

        rubricTemplateService.update(created.id(), new UpdateRubricTemplateRequest(
                "Single",
                null,
                new BigDecimal("10.00"),
                false,
                List.of(
                        new CriterionTemplateInput(0, "First",  null, CriterionKind.BOOLEAN, CriterionRole.PRIMARY,
                                new BigDecimal("4.00"), null, null, null),
                        new CriterionTemplateInput(1, "Second", null, CriterionKind.BOOLEAN, CriterionRole.PRIMARY,
                                new BigDecimal("6.00"), null, null, null)
                )
        ), teacher);

        RubricTemplateDto reloaded = rubricTemplateService.getById(created.id(), teacher.getId());
        assertThat(reloaded.criteria()).hasSize(2);
    }

    @Test
    void update_can_reuse_existing_ordinals_and_swap_kinds() {
        // Бывший SCORE-критерий заменяем на BOOLEAN с теми же ordinal — это тоже триггерило UNIQUE
        RubricTemplateDto created = rubricTemplateService.create(cls.getId(), new CreateRubricTemplateRequest(
                "Mixed",
                null,
                new BigDecimal("10.00"),
                false,
                List.of(
                        new CriterionTemplateInput(0, "S", null, CriterionKind.SCORE, CriterionRole.PRIMARY,
                                new BigDecimal("10.00"), null,
                                new BigDecimal("0.00"), new BigDecimal("5.00"))
                )
        ), teacher);

        rubricTemplateService.update(created.id(), new UpdateRubricTemplateRequest(
                "Mixed",
                null,
                new BigDecimal("10.00"),
                false,
                List.of(
                        new CriterionTemplateInput(0, "B", null, CriterionKind.BOOLEAN, CriterionRole.PRIMARY,
                                new BigDecimal("10.00"), null, null, null)
                )
        ), teacher);

        RubricTemplateDto reloaded = rubricTemplateService.getById(created.id(), teacher.getId());
        assertThat(reloaded.criteria()).hasSize(1);
        assertThat(reloaded.criteria().get(0).kind()).isEqualTo(CriterionKind.BOOLEAN);
        assertThat(reloaded.criteria().get(0).scoreMin()).isNull();
    }
}

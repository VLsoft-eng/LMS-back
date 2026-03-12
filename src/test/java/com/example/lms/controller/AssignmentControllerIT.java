package com.example.lms.controller;

import com.example.lms.AbstractIntegrationTest;
import com.example.lms.entity.AssignmentEntity;
import com.example.lms.entity.ClassEntity;
import com.example.lms.entity.ClassMemberEntity;
import com.example.lms.entity.Role;
import com.example.lms.entity.SubmissionEntity;
import com.example.lms.entity.UserEntity;
import com.example.lms.repository.AssignmentRepository;
import com.example.lms.repository.ClassMemberRepository;
import com.example.lms.repository.ClassRepository;
import com.example.lms.repository.SubmissionRepository;
import com.example.lms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockPart;
import org.springframework.security.test.context.support.WithMockUser;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AssignmentControllerIT extends AbstractIntegrationTest {

    private static final String CLASSES_URL = "/api/v1/classes";
    private static final String ASSIGNMENTS_URL = "/api/v1/assignments";
    private static final String OWNER_EMAIL   = "assign-owner@test.com";
    private static final String TEACHER_EMAIL = "assign-teacher@test.com";
    private static final String STUDENT_EMAIL = "assign-student@test.com";

    @Autowired private UserRepository        userRepository;
    @Autowired private ClassRepository      classRepository;
    @Autowired private ClassMemberRepository classMemberRepository;
    @Autowired private AssignmentRepository  assignmentRepository;
    @Autowired private SubmissionRepository  submissionRepository;

    private UserEntity      owner;
    private UserEntity      teacher;
    private UserEntity      student;
    private ClassEntity     cls;

    @BeforeEach
    void setUp() {
        submissionRepository.deleteAll();
        assignmentRepository.deleteAll();
        classMemberRepository.deleteAll();
        classRepository.deleteAll();
        userRepository.deleteAll();

        owner = userRepository.save(UserEntity.builder()
                .firstName("Owner").lastName("User")
                .email(OWNER_EMAIL).passwordHash("hash")
                .build());
        teacher = userRepository.save(UserEntity.builder()
                .firstName("Teacher").lastName("User")
                .email(TEACHER_EMAIL).passwordHash("hash")
                .build());
        student = userRepository.save(UserEntity.builder()
                .firstName("Student").lastName("User")
                .email(STUDENT_EMAIL).passwordHash("hash")
                .build());

        cls = classRepository.save(ClassEntity.builder()
                .name("Math 101").code("MATH0101").ownerId(owner.getId())
                .build());
        classMemberRepository.save(ClassMemberEntity.builder()
                .classId(cls.getId()).userId(owner.getId()).role(Role.OWNER)
                .build());
        classMemberRepository.save(ClassMemberEntity.builder()
                .classId(cls.getId()).userId(teacher.getId()).role(Role.TEACHER)
                .build());
        classMemberRepository.save(ClassMemberEntity.builder()
                .classId(cls.getId()).userId(student.getId()).role(Role.STUDENT)
                .build());
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void getAssignments_memberOfClass_returns200() throws Exception {
        AssignmentEntity a = assignmentRepository.save(AssignmentEntity.builder()
                .classId(cls.getId())
                .title("Homework 1")
                .description("Solve problems")
                .createdBy(owner.getId())
                .build());

        mockMvc.perform(get(CLASSES_URL + "/" + cls.getId() + "/assignments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(a.getId().toString()))
                .andExpect(jsonPath("$.content[0].title").value("Homework 1"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(username = STUDENT_EMAIL)
    void getAssignments_studentSeesSubmissionStatus_returns200() throws Exception {
        AssignmentEntity a = assignmentRepository.save(AssignmentEntity.builder()
                .classId(cls.getId())
                .title("Homework 1")
                .description("Desc")
                .createdBy(owner.getId())
                .build());
        submissionRepository.save(SubmissionEntity.builder()
                .assignmentId(a.getId())
                .studentId(student.getId())
                .grade((short) 90)
                .build());

        mockMvc.perform(get(CLASSES_URL + "/" + cls.getId() + "/assignments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].submissionStatus").value("GRADED"))
                .andExpect(jsonPath("$.content[0].grade").value(90));
    }

    @Test
    @WithMockUser(username = STUDENT_EMAIL)
    void getAssignments_studentNotSubmitted_returnsNotSubmittedStatus() throws Exception {
        assignmentRepository.save(AssignmentEntity.builder()
                .classId(cls.getId())
                .title("HW")
                .createdBy(owner.getId())
                .build());

        mockMvc.perform(get(CLASSES_URL + "/" + cls.getId() + "/assignments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].submissionStatus").value("NOT_SUBMITTED"))
                .andExpect(jsonPath("$.content[0].grade").isEmpty());
    }

    @Test
    void getAssignments_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get(CLASSES_URL + "/" + cls.getId() + "/assignments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "notmember@test.com")
    void getAssignments_notMember_returns403() throws Exception {
        userRepository.save(UserEntity.builder()
                .firstName("Non").lastName("Member")
                .email("notmember@test.com").passwordHash("hash")
                .build());

        mockMvc.perform(get(CLASSES_URL + "/" + cls.getId() + "/assignments"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void postAssignment_ownerCreates_returns201() throws Exception {
        mockMvc.perform(multipart(CLASSES_URL + "/" + cls.getId() + "/assignments")
                        .part(new MockPart("title", "New Assignment".getBytes(StandardCharsets.UTF_8)))
                        .part(new MockPart("description", "Some desc".getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("New Assignment"))
                .andExpect(jsonPath("$.description").value("Some desc"));
    }

    @Test
    @WithMockUser(username = TEACHER_EMAIL)
    void postAssignment_teacherCreates_returns201() throws Exception {
        mockMvc.perform(multipart(CLASSES_URL + "/" + cls.getId() + "/assignments")
                        .part(new MockPart("title", "Teacher Assignment".getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Teacher Assignment"));
    }

    @Test
    @WithMockUser(username = STUDENT_EMAIL)
    void postAssignment_student_returns403() throws Exception {
        mockMvc.perform(multipart(CLASSES_URL + "/" + cls.getId() + "/assignments")
                        .part(new MockPart("title", "Forbidden".getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void postAssignment_blankTitle_returns400() throws Exception {
        mockMvc.perform(multipart(CLASSES_URL + "/" + cls.getId() + "/assignments")
                        .part(new MockPart("title", "".getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void getAssignment_memberOfClass_returns200() throws Exception {
        AssignmentEntity a = assignmentRepository.save(AssignmentEntity.builder()
                .classId(cls.getId())
                .title("Detail Assignment")
                .description("Full description")
                .createdBy(owner.getId())
                .build());

        mockMvc.perform(get(ASSIGNMENTS_URL + "/" + a.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(a.getId().toString()))
                .andExpect(jsonPath("$.title").value("Detail Assignment"))
                .andExpect(jsonPath("$.description").value("Full description"))
                .andExpect(jsonPath("$.createdByName").value("Owner User"))
                .andExpect(jsonPath("$.classId").value(cls.getId().toString()));
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void getAssignment_notFound_returns404() throws Exception {
        mockMvc.perform(get(ASSIGNMENTS_URL + "/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = STUDENT_EMAIL)
    void getAssignment_studentSeesSubmissionStatus_returns200() throws Exception {
        AssignmentEntity a = assignmentRepository.save(AssignmentEntity.builder()
                .classId(cls.getId())
                .title("HW")
                .createdBy(owner.getId())
                .build());
        submissionRepository.save(SubmissionEntity.builder()
                .assignmentId(a.getId())
                .studentId(student.getId())
                .grade((short) 75)
                .build());

        mockMvc.perform(get(ASSIGNMENTS_URL + "/" + a.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.submissionStatus").value("GRADED"))
                .andExpect(jsonPath("$.grade").value(75));
    }

    @Test
    void getAssignment_unauthenticated_returns401() throws Exception {
        AssignmentEntity a = assignmentRepository.save(AssignmentEntity.builder()
                .classId(cls.getId())
                .title("HW")
                .createdBy(owner.getId())
                .build());

        mockMvc.perform(get(ASSIGNMENTS_URL + "/" + a.getId()))
                .andExpect(status().isUnauthorized());
    }
}

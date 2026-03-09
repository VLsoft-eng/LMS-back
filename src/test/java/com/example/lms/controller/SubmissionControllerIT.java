package com.example.lms.controller;

import com.example.lms.AbstractIntegrationTest;
import com.example.lms.entity.*;
import com.example.lms.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.security.test.context.support.WithMockUser;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SubmissionControllerIT extends AbstractIntegrationTest {

    private static final String OWNER_EMAIL   = "sub-owner@test.com";
    private static final String TEACHER_EMAIL = "sub-teacher@test.com";
    private static final String STUDENT_EMAIL = "sub-student@test.com";

    @Autowired private UserRepository        userRepository;
    @Autowired private ClassRepository       classRepository;
    @Autowired private ClassMemberRepository classMemberRepository;
    @Autowired private AssignmentRepository  assignmentRepository;
    @Autowired private SubmissionRepository  submissionRepository;

    private UserEntity      owner;
    private UserEntity      teacher;
    private UserEntity      student;
    private ClassEntity     cls;
    private AssignmentEntity assignment;

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
                .name("Math 101").code("SUBM0101").ownerId(owner.getId())
                .build());
        classMemberRepository.save(ClassMemberEntity.builder()
                .classId(cls.getId()).userId(owner.getId()).role(Role.OWNER).build());
        classMemberRepository.save(ClassMemberEntity.builder()
                .classId(cls.getId()).userId(teacher.getId()).role(Role.TEACHER).build());
        classMemberRepository.save(ClassMemberEntity.builder()
                .classId(cls.getId()).userId(student.getId()).role(Role.STUDENT).build());

        assignment = assignmentRepository.save(AssignmentEntity.builder()
                .classId(cls.getId())
                .title("Homework 1")
                .description("Solve problems")
                .createdBy(teacher.getId())
                .build());
    }

    // --- POST /api/v1/assignments/{id}/submissions ---

    @Test
    @WithMockUser(username = STUDENT_EMAIL)
    void post_submissions_studentSubmitsText_returns201() throws Exception {
        MockPart answerPart = new MockPart("answerText", "My answer".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/assignments/" + assignment.getId() + "/submissions")
                        .part(answerPart))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.studentId").value(student.getId().toString()))
                .andExpect(jsonPath("$.studentName").value("Student User"))
                .andExpect(jsonPath("$.answerText").value("My answer"))
                .andExpect(jsonPath("$.grade").isEmpty());
    }

    @Test
    @WithMockUser(username = STUDENT_EMAIL)
    void post_submissions_studentSubmitsFile_returns201() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "homework.pdf", "application/pdf", "pdf-content".getBytes());

        mockMvc.perform(multipart("/api/v1/assignments/" + assignment.getId() + "/submissions")
                        .file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileUrl").isNotEmpty());
    }

    @Test
    @WithMockUser(username = STUDENT_EMAIL)
    void post_submissions_studentResubmits_upserts() throws Exception {
        // First submission
        MockPart answer1 = new MockPart("answerText", "First answer".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/v1/assignments/" + assignment.getId() + "/submissions")
                        .part(answer1))
                .andExpect(status().isCreated());

        // Second submission (upsert)
        MockPart answer2 = new MockPart("answerText", "Updated answer".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/v1/assignments/" + assignment.getId() + "/submissions")
                        .part(answer2))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.answerText").value("Updated answer"));

        assertThat(submissionRepository.findAllByAssignmentId(assignment.getId())).hasSize(1);
    }

    @Test
    @WithMockUser(username = TEACHER_EMAIL)
    void post_submissions_teacher_returns403() throws Exception {
        MockPart answer = new MockPart("answerText", "answer".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/assignments/" + assignment.getId() + "/submissions")
                        .part(answer))
                .andExpect(status().isForbidden());
    }

    @Test
    void post_submissions_unauthenticated_returns401() throws Exception {
        mockMvc.perform(multipart("/api/v1/assignments/" + assignment.getId() + "/submissions"))
                .andExpect(status().isUnauthorized());
    }

    // --- GET /api/v1/assignments/{id}/submissions ---

    @Test
    @WithMockUser(username = TEACHER_EMAIL)
    void get_submissions_teacher_returns200() throws Exception {
        submissionRepository.save(SubmissionEntity.builder()
                .assignmentId(assignment.getId())
                .studentId(student.getId())
                .answerText("Student answer")
                .build());

        mockMvc.perform(get("/api/v1/assignments/" + assignment.getId() + "/submissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].studentName").value("Student User"))
                .andExpect(jsonPath("$[0].answerText").value("Student answer"));
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void get_submissions_owner_returns200() throws Exception {
        submissionRepository.save(SubmissionEntity.builder()
                .assignmentId(assignment.getId())
                .studentId(student.getId())
                .answerText("Answer")
                .build());

        mockMvc.perform(get("/api/v1/assignments/" + assignment.getId() + "/submissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentId").value(student.getId().toString()));
    }

    @Test
    @WithMockUser(username = STUDENT_EMAIL)
    void get_submissions_student_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/assignments/" + assignment.getId() + "/submissions"))
                .andExpect(status().isForbidden());
    }

    // --- GET /api/v1/assignments/{id}/submissions/my ---

    @Test
    @WithMockUser(username = STUDENT_EMAIL)
    void get_mySubmission_studentHasSubmission_returns200() throws Exception {
        submissionRepository.save(SubmissionEntity.builder()
                .assignmentId(assignment.getId())
                .studentId(student.getId())
                .answerText("My answer")
                .grade((short) 90)
                .build());

        mockMvc.perform(get("/api/v1/assignments/" + assignment.getId() + "/submissions/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answerText").value("My answer"))
                .andExpect(jsonPath("$.grade").value(90))
                .andExpect(jsonPath("$.studentName").value("Student User"));
    }

    @Test
    @WithMockUser(username = STUDENT_EMAIL)
    void get_mySubmission_noSubmission_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/assignments/" + assignment.getId() + "/submissions/my"))
                .andExpect(status().isNotFound());
    }

    // --- PUT /api/v1/submissions/{id}/grade ---

    @Test
    @WithMockUser(username = TEACHER_EMAIL)
    void put_grade_teacher_returns200() throws Exception {
        SubmissionEntity sub = submissionRepository.save(SubmissionEntity.builder()
                .assignmentId(assignment.getId())
                .studentId(student.getId())
                .answerText("Answer")
                .build());

        mockMvc.perform(put("/api/v1/submissions/" + sub.getId() + "/grade")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"grade\":85}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grade").value(85));
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void put_grade_owner_returns200() throws Exception {
        SubmissionEntity sub = submissionRepository.save(SubmissionEntity.builder()
                .assignmentId(assignment.getId())
                .studentId(student.getId())
                .answerText("Answer")
                .build());

        mockMvc.perform(put("/api/v1/submissions/" + sub.getId() + "/grade")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"grade\":100}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grade").value(100));
    }

    @Test
    @WithMockUser(username = STUDENT_EMAIL)
    void put_grade_student_returns403() throws Exception {
        SubmissionEntity sub = submissionRepository.save(SubmissionEntity.builder()
                .assignmentId(assignment.getId())
                .studentId(student.getId())
                .answerText("Answer")
                .build());

        mockMvc.perform(put("/api/v1/submissions/" + sub.getId() + "/grade")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"grade\":50}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = TEACHER_EMAIL)
    void put_grade_invalidGrade_returns400() throws Exception {
        SubmissionEntity sub = submissionRepository.save(SubmissionEntity.builder()
                .assignmentId(assignment.getId())
                .studentId(student.getId())
                .answerText("Answer")
                .build());

        mockMvc.perform(put("/api/v1/submissions/" + sub.getId() + "/grade")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"grade\":150}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = TEACHER_EMAIL)
    void put_grade_nullGrade_returns400() throws Exception {
        SubmissionEntity sub = submissionRepository.save(SubmissionEntity.builder()
                .assignmentId(assignment.getId())
                .studentId(student.getId())
                .answerText("Answer")
                .build());

        mockMvc.perform(put("/api/v1/submissions/" + sub.getId() + "/grade")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // --- DELETE /api/v1/assignments/{id}/submissions/my ---

    @Test
    @WithMockUser(username = STUDENT_EMAIL)
    void delete_mySubmission_returns204() throws Exception {
        submissionRepository.save(SubmissionEntity.builder()
                .assignmentId(assignment.getId())
                .studentId(student.getId())
                .answerText("To be cancelled")
                .build());

        mockMvc.perform(delete("/api/v1/assignments/" + assignment.getId() + "/submissions/my"))
                .andExpect(status().isNoContent());

        assertThat(submissionRepository.findByAssignmentIdAndStudentId(
                assignment.getId(), student.getId())).isEmpty();
    }

    @Test
    @WithMockUser(username = STUDENT_EMAIL)
    void delete_mySubmission_noSubmission_returns404() throws Exception {
        mockMvc.perform(delete("/api/v1/assignments/" + assignment.getId() + "/submissions/my"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = STUDENT_EMAIL)
    void delete_mySubmission_graded_returns403() throws Exception {
        submissionRepository.save(SubmissionEntity.builder()
                .assignmentId(assignment.getId())
                .studentId(student.getId())
                .answerText("Answer")
                .grade((short) 85)
                .build());

        mockMvc.perform(delete("/api/v1/assignments/" + assignment.getId() + "/submissions/my"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = TEACHER_EMAIL)
    void delete_mySubmission_teacher_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/assignments/" + assignment.getId() + "/submissions/my"))
                .andExpect(status().isForbidden());
    }

    // --- Deadline enforcement ---

    @Test
    @WithMockUser(username = STUDENT_EMAIL)
    void post_submissions_afterDeadline_returns403() throws Exception {
        AssignmentEntity expired = assignmentRepository.save(AssignmentEntity.builder()
                .classId(cls.getId())
                .title("Expired HW")
                .createdBy(teacher.getId())
                .deadline(Instant.now().minusSeconds(3600))
                .build());

        MockPart answer = new MockPart("answerText", "Late".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/v1/assignments/" + expired.getId() + "/submissions")
                        .part(answer))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = STUDENT_EMAIL)
    void delete_mySubmission_afterDeadline_returns403() throws Exception {
        AssignmentEntity expired = assignmentRepository.save(AssignmentEntity.builder()
                .classId(cls.getId())
                .title("Expired HW")
                .createdBy(teacher.getId())
                .deadline(Instant.now().minusSeconds(3600))
                .build());

        submissionRepository.save(SubmissionEntity.builder()
                .assignmentId(expired.getId())
                .studentId(student.getId())
                .answerText("Answer")
                .build());

        mockMvc.perform(delete("/api/v1/assignments/" + expired.getId() + "/submissions/my"))
                .andExpect(status().isForbidden());
    }

    // --- Full cycle: submit → grade → check ---

    @Test
    @WithMockUser(username = STUDENT_EMAIL)
    void fullCycle_submitAndGrade() throws Exception {
        MockPart answer = new MockPart("answerText", "Full cycle answer".getBytes(StandardCharsets.UTF_8));
        String submitResponse = mockMvc.perform(multipart("/api/v1/assignments/" + assignment.getId() + "/submissions")
                        .part(answer))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String submissionId = objectMapper.readTree(submitResponse).get("id").asText();
        assertThat(submissionId).isNotEmpty();
    }
}

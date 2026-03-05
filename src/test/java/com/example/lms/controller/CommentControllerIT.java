package com.example.lms.controller;

import com.example.lms.AbstractIntegrationTest;
import com.example.lms.entity.AssignmentEntity;
import com.example.lms.entity.ClassEntity;
import com.example.lms.entity.ClassMemberEntity;
import com.example.lms.entity.CommentEntity;
import com.example.lms.entity.Role;
import com.example.lms.entity.UserEntity;
import com.example.lms.repository.AssignmentRepository;
import com.example.lms.repository.ClassMemberRepository;
import com.example.lms.repository.ClassRepository;
import com.example.lms.repository.CommentRepository;
import com.example.lms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CommentControllerIT extends AbstractIntegrationTest {

    private static final String ASSIGNMENTS_URL = "/api/v1/assignments";
    private static final String OWNER_EMAIL     = "comment-owner@test.com";
    private static final String TEACHER_EMAIL   = "comment-teacher@test.com";
    private static final String STUDENT_EMAIL   = "comment-student@test.com";

    @Autowired private UserRepository        userRepository;
    @Autowired private ClassRepository       classRepository;
    @Autowired private ClassMemberRepository classMemberRepository;
    @Autowired private AssignmentRepository  assignmentRepository;
    @Autowired private CommentRepository     commentRepository;

    private UserEntity       owner;
    private UserEntity       teacher;
    private UserEntity       student;
    private ClassEntity      cls;
    private AssignmentEntity assignment;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
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
                .name("Math 101").code("COMM0101").ownerId(owner.getId())
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

        assignment = assignmentRepository.save(AssignmentEntity.builder()
                .classId(cls.getId())
                .title("Homework 1")
                .description("Solve problems")
                .createdBy(owner.getId())
                .build());
    }

    // ── GET /assignments/{id}/comments ─────────────────────────────

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void getComments_memberOfClass_returns200() throws Exception {
        commentRepository.save(CommentEntity.builder()
                .assignmentId(assignment.getId())
                .authorId(student.getId())
                .text("First comment")
                .build());
        commentRepository.save(CommentEntity.builder()
                .assignmentId(assignment.getId())
                .authorId(owner.getId())
                .text("Second comment")
                .build());

        mockMvc.perform(get(ASSIGNMENTS_URL + "/" + assignment.getId() + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].text").value("First comment"))
                .andExpect(jsonPath("$.content[0].authorName").value("Student User"))
                .andExpect(jsonPath("$.content[1].text").value("Second comment"))
                .andExpect(jsonPath("$.content[1].authorName").value("Owner User"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @WithMockUser(username = STUDENT_EMAIL)
    void getComments_studentMember_returns200() throws Exception {
        commentRepository.save(CommentEntity.builder()
                .assignmentId(assignment.getId())
                .authorId(owner.getId())
                .text("Teacher comment")
                .build());

        mockMvc.perform(get(ASSIGNMENTS_URL + "/" + assignment.getId() + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].text").value("Teacher comment"));
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void getComments_emptyList_returns200() throws Exception {
        mockMvc.perform(get(ASSIGNMENTS_URL + "/" + assignment.getId() + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void getComments_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get(ASSIGNMENTS_URL + "/" + assignment.getId() + "/comments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "notmember@test.com")
    void getComments_notMember_returns403() throws Exception {
        userRepository.save(UserEntity.builder()
                .firstName("Non").lastName("Member")
                .email("notmember@test.com").passwordHash("hash")
                .build());

        mockMvc.perform(get(ASSIGNMENTS_URL + "/" + assignment.getId() + "/comments"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void getComments_assignmentNotFound_returns404() throws Exception {
        mockMvc.perform(get(ASSIGNMENTS_URL + "/" + UUID.randomUUID() + "/comments"))
                .andExpect(status().isNotFound());
    }

    // ── POST /assignments/{id}/comments ────────────────────────────

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void postComment_owner_returns201() throws Exception {
        mockMvc.perform(post(ASSIGNMENTS_URL + "/" + assignment.getId() + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Great job everyone!\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.text").value("Great job everyone!"))
                .andExpect(jsonPath("$.authorName").value("Owner User"))
                .andExpect(jsonPath("$.authorId").value(owner.getId().toString()))
                .andExpect(jsonPath("$.assignmentId").value(assignment.getId().toString()));
    }

    @Test
    @WithMockUser(username = TEACHER_EMAIL)
    void postComment_teacher_returns201() throws Exception {
        mockMvc.perform(post(ASSIGNMENTS_URL + "/" + assignment.getId() + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Please review chapter 3\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("Please review chapter 3"))
                .andExpect(jsonPath("$.authorName").value("Teacher User"));
    }

    @Test
    @WithMockUser(username = STUDENT_EMAIL)
    void postComment_student_returns201() throws Exception {
        mockMvc.perform(post(ASSIGNMENTS_URL + "/" + assignment.getId() + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"I have a question about task 2\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("I have a question about task 2"))
                .andExpect(jsonPath("$.authorName").value("Student User"));
    }

    @Test
    void postComment_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post(ASSIGNMENTS_URL + "/" + assignment.getId() + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Hello\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "notmember@test.com")
    void postComment_notMember_returns403() throws Exception {
        userRepository.save(UserEntity.builder()
                .firstName("Non").lastName("Member")
                .email("notmember@test.com").passwordHash("hash")
                .build());

        mockMvc.perform(post(ASSIGNMENTS_URL + "/" + assignment.getId() + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Should not work\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void postComment_assignmentNotFound_returns404() throws Exception {
        mockMvc.perform(post(ASSIGNMENTS_URL + "/" + UUID.randomUUID() + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Not found\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void postComment_blankText_returns400() throws Exception {
        mockMvc.perform(post(ASSIGNMENTS_URL + "/" + assignment.getId() + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void postComment_nullText_returns400() throws Exception {
        mockMvc.perform(post(ASSIGNMENTS_URL + "/" + assignment.getId() + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}

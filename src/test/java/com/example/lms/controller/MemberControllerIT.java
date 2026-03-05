package com.example.lms.controller;

import com.example.lms.AbstractIntegrationTest;
import com.example.lms.entity.ClassEntity;
import com.example.lms.entity.ClassMemberEntity;
import com.example.lms.entity.Role;
import com.example.lms.entity.UserEntity;
import com.example.lms.repository.ClassMemberRepository;
import com.example.lms.repository.ClassRepository;
import com.example.lms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MemberControllerIT extends AbstractIntegrationTest {

    private static final String BASE_URL    = "/api/v1/classes";
    private static final String OWNER_EMAIL = "member-owner@test.com";
    private static final String OTHER_EMAIL = "member-other@test.com";

    @Autowired private UserRepository        userRepository;
    @Autowired private ClassRepository       classRepository;
    @Autowired private ClassMemberRepository classMemberRepository;

    private UserEntity   owner;
    private UserEntity   other;
    private ClassEntity  cls;

    @BeforeEach
    void setUp() {
        classMemberRepository.deleteAll();
        classRepository.deleteAll();
        userRepository.deleteAll();

        owner = userRepository.save(UserEntity.builder()
                .firstName("Owner").lastName("User")
                .email(OWNER_EMAIL).passwordHash("hash")
                .build());
        other = userRepository.save(UserEntity.builder()
                .firstName("Other").lastName("User")
                .email(OTHER_EMAIL).passwordHash("hash")
                .build());

        cls = classRepository.save(ClassEntity.builder()
                .name("Math 101").code("MATH0101").ownerId(owner.getId())
                .build());
        classMemberRepository.save(ClassMemberEntity.builder()
                .classId(cls.getId()).userId(owner.getId()).role(Role.OWNER)
                .build());
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void getMembers_memberOfClass_returns200() throws Exception {
        classMemberRepository.save(ClassMemberEntity.builder()
                .classId(cls.getId()).userId(other.getId()).role(Role.STUDENT)
                .build());

        mockMvc.perform(get(BASE_URL + "/" + cls.getId() + "/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].userId").isNotEmpty())
                .andExpect(jsonPath("$[0].firstName").isNotEmpty())
                .andExpect(jsonPath("$[0].role").value("OWNER"))
                .andExpect(jsonPath("$[1].role").value("STUDENT"));
    }

    @Test
    @WithMockUser(username = OTHER_EMAIL)
    void getMembers_notMember_returns403() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + cls.getId() + "/members"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMembers_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + cls.getId() + "/members"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void getMembers_classNotFound_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + UUID.randomUUID() + "/members"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void putMemberRole_ownerAssignsTeacher_returns200() throws Exception {
        classMemberRepository.save(ClassMemberEntity.builder()
                .classId(cls.getId()).userId(other.getId()).role(Role.STUDENT)
                .build());

        mockMvc.perform(put(BASE_URL + "/" + cls.getId() + "/members/" + other.getId() + "/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"TEACHER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(other.getId().toString()))
                .andExpect(jsonPath("$.role").value("TEACHER"));
    }

    @Test
    @WithMockUser(username = OTHER_EMAIL)
    void putMemberRole_nonOwner_returns403() throws Exception {
        classMemberRepository.save(ClassMemberEntity.builder()
                .classId(cls.getId()).userId(other.getId()).role(Role.STUDENT)
                .build());
        UserEntity third = userRepository.save(UserEntity.builder()
                .firstName("Third").lastName("User")
                .email("third@test.com").passwordHash("hash")
                .build());
        classMemberRepository.save(ClassMemberEntity.builder()
                .classId(cls.getId()).userId(third.getId()).role(Role.STUDENT)
                .build());

        mockMvc.perform(put(BASE_URL + "/" + cls.getId() + "/members/" + third.getId() + "/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"TEACHER\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void putMemberRole_cannotChangeOwnerRole_returns403() throws Exception {
        mockMvc.perform(put(BASE_URL + "/" + cls.getId() + "/members/" + owner.getId() + "/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"TEACHER\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void putMemberRole_cannotAssignOwner_returns400() throws Exception {
        classMemberRepository.save(ClassMemberEntity.builder()
                .classId(cls.getId()).userId(other.getId()).role(Role.STUDENT)
                .build());

        mockMvc.perform(put(BASE_URL + "/" + cls.getId() + "/members/" + other.getId() + "/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"OWNER\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void putMemberRole_memberNotFound_returns404() throws Exception {
        UserEntity nonMember = userRepository.save(UserEntity.builder()
                .firstName("Non").lastName("Member")
                .email("nonmember@test.com").passwordHash("hash")
                .build());

        mockMvc.perform(put(BASE_URL + "/" + cls.getId() + "/members/" + nonMember.getId() + "/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"TEACHER\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void putMemberRole_blankRole_returns400() throws Exception {
        classMemberRepository.save(ClassMemberEntity.builder()
                .classId(cls.getId()).userId(other.getId()).role(Role.STUDENT)
                .build());

        mockMvc.perform(put(BASE_URL + "/" + cls.getId() + "/members/" + other.getId() + "/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}

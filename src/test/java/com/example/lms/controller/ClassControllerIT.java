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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ClassControllerIT extends AbstractIntegrationTest {

    private static final String BASE_URL    = "/api/v1/classes";
    private static final String OWNER_EMAIL = "class-owner@test.com";
    private static final String OTHER_EMAIL = "class-other@test.com";

    @Autowired private UserRepository        userRepository;
    @Autowired private ClassRepository       classRepository;
    @Autowired private ClassMemberRepository classMemberRepository;

    private UserEntity owner;
    private UserEntity other;

    @BeforeEach
    void setUp() {
        classMemberRepository.deleteAll();
        classRepository.deleteAll();
        userRepository.deleteAll();

        owner = userRepository.save(UserEntity.builder()
                .firstName("Ivan").lastName("Owner")
                .email(OWNER_EMAIL).passwordHash("hash")
                .build());

        other = userRepository.save(UserEntity.builder()
                .firstName("Other").lastName("User")
                .email(OTHER_EMAIL).passwordHash("hash")
                .build());
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void postClass_validRequest_returns201() throws Exception {
        // Given / When / Then
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Math 101\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Math 101"))
                .andExpect(jsonPath("$.code").isNotEmpty())
                .andExpect(jsonPath("$.myRole").value("OWNER"))
                .andExpect(jsonPath("$.memberCount").value(1));
    }

    @Test
    void postClass_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Math 101\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void postClass_blankName_returns400() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void getClasses_returnsMyClassesWithMyRole_returns200() throws Exception {
        // Given
        ClassEntity cls = classRepository.save(ClassEntity.builder()
                .name("Physics").code("PHYS0001").ownerId(owner.getId()).build());
        classMemberRepository.save(ClassMemberEntity.builder()
                .classId(cls.getId()).userId(owner.getId()).role(Role.OWNER).build());

        // When / Then
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].name").value("Physics"))
                .andExpect(jsonPath("$.content[0].myRole").value("OWNER"))
                .andExpect(jsonPath("$.content[0].memberCount").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getClasses_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = OTHER_EMAIL)
    void postJoin_validCode_returns200WithStudentRole() throws Exception {
        // Given
        ClassEntity cls = classRepository.save(ClassEntity.builder()
                .name("History").code("HIST0001").ownerId(owner.getId()).build());
        classMemberRepository.save(ClassMemberEntity.builder()
                .classId(cls.getId()).userId(owner.getId()).role(Role.OWNER).build());

        // When / Then
        mockMvc.perform(post(BASE_URL + "/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"HIST0001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("History"))
                .andExpect(jsonPath("$.myRole").value("STUDENT"));
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void postJoin_invalidCode_returns404() throws Exception {
        mockMvc.perform(post(BASE_URL + "/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"XXXXXXXX\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void postJoin_alreadyMember_returns409() throws Exception {
        // Given
        ClassEntity cls = classRepository.save(ClassEntity.builder()
                .name("Art").code("ARTT0001").ownerId(owner.getId()).build());
        classMemberRepository.save(ClassMemberEntity.builder()
                .classId(cls.getId()).userId(owner.getId()).role(Role.OWNER).build());

        // When / Then
        mockMvc.perform(post(BASE_URL + "/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ARTT0001\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void putClass_ownerCanRename_returns200() throws Exception {
        // Given
        ClassEntity cls = classRepository.save(ClassEntity.builder()
                .name("Old Name").code("RENAME01").ownerId(owner.getId()).build());
        classMemberRepository.save(ClassMemberEntity.builder()
                .classId(cls.getId()).userId(owner.getId()).role(Role.OWNER).build());

        // When / Then
        mockMvc.perform(put(BASE_URL + "/" + cls.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New Name\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.myRole").value("OWNER"));
    }

    @Test
    @WithMockUser(username = OTHER_EMAIL)
    void putClass_nonOwner_returns403() throws Exception {
        // Given
        ClassEntity cls = classRepository.save(ClassEntity.builder()
                .name("Old Name").code("RENAME02").ownerId(owner.getId()).build());
        classMemberRepository.save(ClassMemberEntity.builder()
                .classId(cls.getId()).userId(owner.getId()).role(Role.OWNER).build());
        classMemberRepository.save(ClassMemberEntity.builder()
                .classId(cls.getId()).userId(other.getId()).role(Role.STUDENT).build());

        // When / Then
        mockMvc.perform(put(BASE_URL + "/" + cls.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New Name\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void putClass_notFound_returns404() throws Exception {
        mockMvc.perform(put(BASE_URL + "/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New Name\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void deleteClass_owner_returns204() throws Exception {
        // Given
        ClassEntity cls = classRepository.save(ClassEntity.builder()
                .name("Music").code("MUSC0001").ownerId(owner.getId()).build());
        classMemberRepository.save(ClassMemberEntity.builder()
                .classId(cls.getId()).userId(owner.getId()).role(Role.OWNER).build());

        // When / Then
        mockMvc.perform(delete(BASE_URL + "/" + cls.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = OTHER_EMAIL)
    void deleteClass_nonOwner_returns403() throws Exception {
        // Given
        ClassEntity cls = classRepository.save(ClassEntity.builder()
                .name("Sport").code("SPRT0001").ownerId(owner.getId()).build());
        classMemberRepository.save(ClassMemberEntity.builder()
                .classId(cls.getId()).userId(owner.getId()).role(Role.OWNER).build());
        classMemberRepository.save(ClassMemberEntity.builder()
                .classId(cls.getId()).userId(other.getId()).role(Role.STUDENT).build());

        // When / Then
        mockMvc.perform(delete(BASE_URL + "/" + cls.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void deleteClass_notFound_returns404() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL)
    void getClassCode_ownerCanAccess_returns200() throws Exception {
        // Given
        ClassEntity cls = classRepository.save(ClassEntity.builder()
                .name("Biology").code("BIOL0001").ownerId(owner.getId()).build());
        classMemberRepository.save(ClassMemberEntity.builder()
                .classId(cls.getId()).userId(owner.getId()).role(Role.OWNER).build());

        // When / Then
        mockMvc.perform(get(BASE_URL + "/" + cls.getId() + "/code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("BIOL0001"));
    }

    @Test
    @WithMockUser(username = OTHER_EMAIL)
    void getClassCode_studentCannotAccess_returns403() throws Exception {
        // Given
        ClassEntity cls = classRepository.save(ClassEntity.builder()
                .name("Chemistry").code("CHEM0001").ownerId(owner.getId()).build());
        classMemberRepository.save(ClassMemberEntity.builder()
                .classId(cls.getId()).userId(owner.getId()).role(Role.OWNER).build());
        classMemberRepository.save(ClassMemberEntity.builder()
                .classId(cls.getId()).userId(other.getId()).role(Role.STUDENT).build());

        // When / Then
        mockMvc.perform(get(BASE_URL + "/" + cls.getId() + "/code"))
                .andExpect(status().isForbidden());
    }
}

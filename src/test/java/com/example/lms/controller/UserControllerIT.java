package com.example.lms.controller;

import com.example.lms.AbstractIntegrationTest;
import com.example.lms.dto.UpdateProfileRequest;
import com.example.lms.entity.UserEntity;
import com.example.lms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerIT extends AbstractIntegrationTest {

    private static final String TEST_EMAIL = "it-user@test.com";

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        userRepository.save(UserEntity.builder()
                .firstName("Ivan")
                .lastName("Ivanov")
                .email(TEST_EMAIL)
                .passwordHash("hashed_password")
                .build());
    }

    // ─── GET /api/v1/users/me ────────────────────────────────────────────────

    @Test
    @WithMockUser(username = TEST_EMAIL)
    void getMe_authenticatedUser_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                .andExpect(jsonPath("$.firstName").value("Ivan"))
                .andExpect(jsonPath("$.lastName").value("Ivanov"));
    }

    @Test
    void getMe_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    // ─── PUT /api/v1/users/me ────────────────────────────────────────────────

    @Test
    @WithMockUser(username = TEST_EMAIL)
    void putMe_validRequest_returns200() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest(
                "Petr", "Petrov", "https://img.example.com/avatar.png", LocalDate.of(1990, 5, 15));

        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Petr"))
                .andExpect(jsonPath("$.lastName").value("Petrov"))
                .andExpect(jsonPath("$.avatarUrl").value("https://img.example.com/avatar.png"))
                .andExpect(jsonPath("$.email").value(TEST_EMAIL));
    }

    @Test
    @WithMockUser(username = TEST_EMAIL)
    void putMe_blankFirstName_returns400() throws Exception {
        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"\",\"lastName\":\"Petrov\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = TEST_EMAIL)
    void putMe_blankLastName_returns400() throws Exception {
        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Petr\",\"lastName\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void putMe_unauthenticated_returns401() throws Exception {
        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Petr\",\"lastName\":\"Petrov\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = TEST_EMAIL)
    void putMe_emailFieldIgnored_returns200WithOriginalEmail() throws Exception {
        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Petr\",\"lastName\":\"Petrov\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(TEST_EMAIL));
    }
}

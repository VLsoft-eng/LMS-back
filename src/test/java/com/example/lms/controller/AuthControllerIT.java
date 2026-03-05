package com.example.lms.controller;

import com.example.lms.AbstractIntegrationTest;
import com.example.lms.dto.LoginRequest;
import com.example.lms.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerIT extends AbstractIntegrationTest {

    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String LOGIN_URL    = "/api/v1/auth/login";

    @Test
    void postRegister_validRequest_returns201() throws Exception {
        RegisterRequest body = new RegisterRequest(
                "Ivan", "Ivanov", "ivan@test.com", "password123", null, null);

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("ivan@test.com"))
                .andExpect(jsonPath("$.user.firstName").value("Ivan"))
                .andExpect(jsonPath("$.user.lastName").value("Ivanov"));
    }

    @Test
    void postRegister_duplicateEmail_returns409() throws Exception {
        RegisterRequest body = new RegisterRequest(
                "Ivan", "Ivanov", "dup@test.com", "password123", null, null);

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void postRegister_blankFirstName_returns400() throws Exception {
        RegisterRequest body = new RegisterRequest(
                "", "Ivanov", "valid@test.com", "password123", null, null);

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postRegister_invalidEmail_returns400() throws Exception {
        RegisterRequest body = new RegisterRequest(
                "Ivan", "Ivanov", "not-an-email", "password123", null, null);

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postRegister_shortPassword_returns400() throws Exception {
        RegisterRequest body = new RegisterRequest(
                "Ivan", "Ivanov", "ivan2@test.com", "short", null, null);

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postLogin_validCredentials_returns200WithToken() throws Exception {
        RegisterRequest reg = new RegisterRequest(
                "Maria", "Petrova", "maria@test.com", "mypassword1", null, null);
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest("maria@test.com", "mypassword1");
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("maria@test.com"));
    }

    @Test
    void postLogin_wrongPassword_returns401() throws Exception {
        RegisterRequest reg = new RegisterRequest(
                "Petr", "Sidorov", "petr@test.com", "correctPass1", null, null);
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest("petr@test.com", "wrongPass");
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void postLogin_unknownEmail_returns401() throws Exception {
        LoginRequest login = new LoginRequest("nobody@test.com", "password123");

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void postLogin_blankEmail_returns400() throws Exception {
        LoginRequest login = new LoginRequest("", "password123");

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isBadRequest());
    }
}

package com.example.lms.controller;

import com.example.lms.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CorsIT extends AbstractIntegrationTest {

    @Test
    void options_preflightRequest_returnsAllowOriginHeader() throws Exception {
        mockMvc.perform(options("/api/v1/auth/register")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type,Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"))
                .andExpect(header().exists("Access-Control-Allow-Methods"));
    }

    @Test
    void options_preflightRequest_allowsGetMethod() throws Exception {
        mockMvc.perform(options("/api/v1/classes")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }

    @Test
    void options_preflightRequest_allowsDeleteMethod() throws Exception {
        mockMvc.perform(options("/api/v1/classes/some-id")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "DELETE"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }

    @Test
    void options_preflightRequest_allowsAuthorizationHeader() throws Exception {
        mockMvc.perform(options("/api/v1/users/me")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Headers"));
    }
}

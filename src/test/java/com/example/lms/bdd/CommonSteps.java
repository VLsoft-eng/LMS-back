package com.example.lms.bdd;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TICKET #9185: common step definitions — auth, class setup, data cleanup.
 */
@RequiredArgsConstructor
public class CommonSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PeerReviewTestContext ctx;

    protected final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Before
    public void cleanDatabase() {
        ctx.reset();
        // Clean peer-review tables first (FK order)
        jdbc.execute("DELETE FROM peer_criterion_scores");
        jdbc.execute("DELETE FROM peer_assessments");
        jdbc.execute("DELETE FROM peer_review_assignments");
        jdbc.execute("DELETE FROM peer_review_settings");
        jdbc.execute("DELETE FROM criterion_scores");
        jdbc.execute("DELETE FROM assessments");
        jdbc.execute("DELETE FROM criteria");
        jdbc.execute("DELETE FROM rubrics");
        jdbc.execute("DELETE FROM criterion_templates");
        jdbc.execute("DELETE FROM rubric_templates");
        jdbc.execute("DELETE FROM submissions");
        jdbc.execute("DELETE FROM assignments");
        jdbc.execute("DELETE FROM class_members");
        jdbc.execute("DELETE FROM classes");
        jdbc.execute("DELETE FROM users");
    }

    // ─── Auth helpers ───────────────────────────────────────────────────────

    protected String registerAndLogin(String email, String firstName, String lastName) throws Exception {
        // Register
        var regBody = Map.of(
                "email", email,
                "password", "Test1234!",
                "firstName", firstName,
                "lastName", lastName
        );
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(regBody)));

        // Login
        var loginBody = Map.of("email", email, "password", "Test1234!");
        var result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginBody)))
                .andExpect(status().isOk())
                .andReturn();

        var response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.path("token").asText();
    }

    protected UUID getUserIdFromToken(String token) throws Exception {
        var result = mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        return UUID.fromString(
                objectMapper.readTree(result.getResponse().getContentAsString())
                        .path("id").asText());
    }
}

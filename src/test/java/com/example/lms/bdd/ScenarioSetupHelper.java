package com.example.lms.bdd;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.cucumber.spring.ScenarioScope;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TICKET #9185: helper component for setting up test data across Cucumber scenarios.
 */
@Component
public class ScenarioSetupHelper {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    /**
     * Creates a class, teacher, and N students.
     * Populates ctx.teacherToken, ctx.classId, ctx.studentTokens, ctx.studentIds.
     */
    public void createClassWithStudents(PeerReviewTestContext ctx, int studentCount) throws Exception {
        // Register + login teacher
        ctx.teacherToken = registerAndLogin("teacher@test.com", "Teacher", "One");
        ctx.teacherUserId = getUserId(ctx.teacherToken);

        // Create class
        var result = mockMvc.perform(post("/api/v1/classes")
                .header("Authorization", "Bearer " + ctx.teacherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Test Class\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        ctx.classId = UUID.fromString(
                mapper.readTree(result.getResponse().getContentAsString()).path("id").asText());

        // Get class join code
        var codeResult = mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/classes/" + ctx.classId + "/code")
                        .header("Authorization", "Bearer " + ctx.teacherToken))
                .andReturn();
        String code = mapper.readTree(codeResult.getResponse().getContentAsString())
                .path("code").asText();

        // Register and enroll students
        String[] names = {"Alice", "Bob", "Carol", "Dave", "Eve"};
        for (int i = 0; i < studentCount; i++) {
            String name = i < names.length ? names[i] : "Student" + i;
            String email = name.toLowerCase() + "@test.com";
            String token = registerAndLogin(email, name, "Test");
            UUID id = getUserId(token);

            ctx.studentTokens.put(name, token);
            ctx.studentIds.put(name, id);
            if (ctx.studentToken == null) {
                ctx.studentToken = token;
                ctx.studentUserId = id;
            }

            // Join class
            mockMvc.perform(post("/api/v1/classes/join")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"code\": \"" + code + "\"}"))
                    .andExpect(status().isOk());
        }
    }

    /**
     * Creates a standard STANDARD assignment. Populates ctx.assignmentId.
     */
    public void createAssignment(PeerReviewTestContext ctx) throws Exception {
        var result = mockMvc.perform(post("/api/v1/classes/" + ctx.classId + "/assignments")
                .header("Authorization", "Bearer " + ctx.teacherToken)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("title", "BDD Assignment")
                .param("isTeamBased", "false"))
                .andReturn();
        ctx.assignmentId = UUID.fromString(
                mapper.readTree(result.getResponse().getContentAsString()).path("id").asText());
    }

    /**
     * Creates and attaches an ad-hoc rubric with 2 criteria (boolean + score).
     * Populates ctx.rubricId.
     */
    public void attachRubric(PeerReviewTestContext ctx) throws Exception {
        String rubricBody = """
                {
                  "adhoc": {
                    "name": "BDD Rubric",
                    "totalMaxPoints": 10.0,
                    "allowOvercap": false,
                    "criteria": [
                      {
                        "ordinal": 0,
                        "title": "Completion",
                        "kind": "BOOLEAN",
                        "role": "PRIMARY",
                        "maxPoints": 5.0
                      },
                      {
                        "ordinal": 1,
                        "title": "Quality",
                        "kind": "SCORE",
                        "role": "PRIMARY",
                        "maxPoints": 5.0,
                        "scoreMin": 0.0,
                        "scoreMax": 5.0
                      }
                    ]
                  }
                }
                """;
        var result = mockMvc.perform(post("/api/v1/assignments/" + ctx.assignmentId + "/rubric")
                .header("Authorization", "Bearer " + ctx.teacherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(rubricBody))
                .andReturn();
        ctx.rubricId = UUID.fromString(
                mapper.readTree(result.getResponse().getContentAsString()).path("id").asText());
    }

    /**
     * Submits work for a named student. Populates ctx.submissionIds.
     */
    public void submitWork(PeerReviewTestContext ctx, String studentName) throws Exception {
        String token = ctx.studentTokens.get(studentName);
        var result = mockMvc.perform(post("/api/v1/assignments/" + ctx.assignmentId + "/submissions")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("answerText", "My answer by " + studentName))
                .andReturn();
        UUID submissionId = UUID.fromString(
                mapper.readTree(result.getResponse().getContentAsString()).path("id").asText());
        ctx.submissionIds.put(studentName, submissionId);
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private String registerAndLogin(String email, String firstName, String lastName) throws Exception {
        var regBody = Map.of("email", email, "password", "Test1234!",
                "firstName", firstName, "lastName", lastName);
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(regBody)));

        var loginBody = Map.of("email", email, "password", "Test1234!");
        var result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(loginBody)))
                .andExpect(status().isOk())
                .andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).path("token").asText();
    }

    private UUID getUserId(String token) throws Exception {
        var result = mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        return UUID.fromString(
                mapper.readTree(result.getResponse().getContentAsString()).path("id").asText());
    }
}

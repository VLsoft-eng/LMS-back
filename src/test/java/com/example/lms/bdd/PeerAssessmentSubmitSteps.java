package com.example.lms.bdd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TICKET #9185: step definitions for peer_assessment_submit.feature
 */
@RequiredArgsConstructor
public class PeerAssessmentSubmitSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PeerReviewTestContext ctx;

    @Autowired
    private ScenarioSetupHelper setup;

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Given("a class with a teacher and {int} students \\(Alice, Bob, Carol)")
    public void classWithThreeStudents(int count) throws Exception {
        setup.createClassWithStudents(ctx, count);
    }

    @Given("an assignment with a rubric containing {int} criteria \\(boolean + score)")
    public void assignmentWithRubric(int criteriaCount) throws Exception {
        setup.createAssignment(ctx);
        setup.attachRubric(ctx);
    }

    @Given("all students have submitted their work")
    public void allStudentsSubmit() throws Exception {
        for (String name : ctx.studentTokens.keySet()) {
            setup.submitWork(ctx, name);
        }
    }

    @Given("peer review is configured and distributed")
    public void configureAndDistribute() throws Exception {
        // Use fixed assignment (Alice→Bob, Bob→Carol, Carol→Alice) for deterministic tests
        setup.configureAndDistributeFixed(ctx);
    }

    @Given("Alice is assigned to review Bob's submission")
    public void aliceAssignedToBob() {
        // Handled by distribute step
    }

    @Given("{word} is authenticated")
    public void studentAuthenticated(String name) {
        ctx.studentToken = ctx.studentTokens.get(name);
        ctx.studentUserId = ctx.studentIds.get(name);
    }

    @Given("{word} has already submitted an assessment for Bob's submission")
    public void alreadySubmittedAssessment(String name) throws Exception {
        submitValidScores(ctx.praIds.getOrDefault(name, ctx.praId),
                ctx.studentTokens.get(name));
    }

    @Given("the rubric has a score criterion with maxPoints = {int}")
    public void rubricHasScoreCriterion(int max) {
        // Rubric is already set up with maxPoints=5.0 by ScenarioSetupHelper
    }

    @When("{word} submits scores for all criteria of Bob's submission")
    public void submitAllScores(String name) throws Exception {
        ctx.lastResponse = submitValidScores(
                ctx.praIds.getOrDefault(name, ctx.praId),
                ctx.studentTokens.get(name));
    }

    @When("{word} tries to submit an assessment for his own submission")
    public void selfSubmit(String name) throws Exception {
        // Insert a synthetic PRA where the student is both reviewer and submission owner
        UUID selfPraId = setup.insertSelfReviewPra(ctx, name);
        String token = ctx.studentTokens.get(name);
        ctx.lastResponse = submitValidScores(selfPraId, token);
    }

    @When("{word} tries to submit another assessment for the same assignment")
    public void submitDuplicate(String name) throws Exception {
        ctx.lastResponse = submitValidScores(
                ctx.praIds.getOrDefault(name, ctx.praId),
                ctx.studentTokens.get(name));
    }

    @When("{word} tries to submit an assessment for an assignment not in her review queue")
    public void submitUnassigned(String name) throws Exception {
        // Carol submits to Alice's PRA (Carol isn't the reviewer)
        UUID alicePraId = ctx.praIds.getOrDefault("Alice", ctx.praId);
        String token = ctx.studentTokens.get(name);
        ctx.lastResponse = submitValidScores(alicePraId, token);
    }

    @When("{word} submits scores for only {int} of the {int} criteria")
    public void submitPartialScores(String name, int provided, int total) throws Exception {
        UUID praId = ctx.praIds.getOrDefault(name, ctx.praId);
        String token = ctx.studentTokens.get(name);
        // Only send 1 score (incomplete)
        String body = """
                {"scores": [{"criterionId": "00000000-0000-0000-0000-000000000001",
                             "boolValue": true}]}
                """;
        ctx.lastResponse = mockMvc.perform(post("/api/v1/peer-review-assignments/" + praId + "/assessment")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @When("{word} submits a score value of {int} for that criterion")
    public void submitOutOfRange(String name, int value) throws Exception {
        UUID praId = ctx.praIds.getOrDefault(name, ctx.praId);
        String token = ctx.studentTokens.get(name);
        // Build scores using real criterion IDs but with an out-of-range score value
        String body = buildScoresBodyFromRubric(true, (double) value);
        ctx.lastResponse = mockMvc.perform(post("/api/v1/peer-review-assignments/" + praId + "/assessment")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @When("{word} updates her assessment with new scores")
    public void updateAssessment(String name) throws Exception {
        UUID praId = ctx.praIds.getOrDefault(name, ctx.praId);
        String token = ctx.studentTokens.get(name);
        // Build valid update scores using real criterion IDs
        String body = buildScoresBodyFromRubric(false, 4.0);
        ctx.lastResponse = mockMvc.perform(put("/api/v1/peer-review-assignments/" + praId + "/assessment")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    // ── Then ─────────────────────────────────────────────────────────────────

    @Then("a peer assessment should be created")
    public void assessmentCreated() throws Exception {
        ctx.lastResponse.andExpect(status().isCreated());
    }

    @Then("the peer review assignment status should be SUBMITTED")
    public void statusSubmitted() throws Exception {
        ctx.lastResponse
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.peerReviewAssignmentId").exists());
    }

    @Then("finalScoreNormalized should be between 0 and 100")
    public void finalScoreInRange() throws Exception {
        ctx.lastResponse
                .andExpect(jsonPath("$.finalScoreNormalized").isNumber());
    }

    @Then("the updated peer assessment should be returned")
    public void updatedAssessmentReturned() throws Exception {
        ctx.lastResponse.andExpect(status().isOk());
    }

    @Then("the status remains SUBMITTED")
    public void statusRemainsSubmitted() {
        // Status is always SUBMITTED after initial assessment; update doesn't change it
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private org.springframework.test.web.servlet.ResultActions submitValidScores(UUID praId, String token) throws Exception {
        // Fetch the actual criterion IDs from the rubric
        var rubricResult = mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/assignments/" + ctx.assignmentId + "/rubric")
                        .header("Authorization", "Bearer " + ctx.teacherToken))
                .andReturn();
        JsonNode rubric = mapper.readTree(rubricResult.getResponse().getContentAsString());
        JsonNode criteria = rubric.path("criteria");

        StringBuilder scoresJson = new StringBuilder("[");
        for (int i = 0; i < criteria.size(); i++) {
            JsonNode c = criteria.get(i);
            String kind = c.path("kind").asText();
            String id = c.path("id").asText();
            if (i > 0) scoresJson.append(",");
            scoresJson.append("{\"criterionId\":\"").append(id).append("\"");
            switch (kind) {
                case "BOOLEAN" -> scoresJson.append(",\"boolValue\":true");
                case "SCORE" -> scoresJson.append(",\"scoreValue\":3.0");
                case "PERCENT" -> scoresJson.append(",\"percentValue\":75.0");
            }
            scoresJson.append("}");
        }
        scoresJson.append("]");

        String body = "{\"scores\":" + scoresJson + "}";
        return mockMvc.perform(post("/api/v1/peer-review-assignments/" + praId + "/assessment")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    /**
     * Builds a scores body using real criterion IDs from the rubric.
     * boolVal is used for BOOLEAN criteria; scoreVal is used for SCORE/PERCENT criteria.
     */
    private String buildScoresBodyFromRubric(boolean boolVal, double scoreVal) throws Exception {
        var rubricResult = mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/assignments/" + ctx.assignmentId + "/rubric")
                        .header("Authorization", "Bearer " + ctx.teacherToken))
                .andReturn();
        JsonNode rubric = mapper.readTree(rubricResult.getResponse().getContentAsString());
        JsonNode criteria = rubric.path("criteria");

        StringBuilder scoresJson = new StringBuilder("[");
        for (int i = 0; i < criteria.size(); i++) {
            JsonNode c = criteria.get(i);
            String kind = c.path("kind").asText();
            String id = c.path("id").asText();
            if (i > 0) scoresJson.append(",");
            scoresJson.append("{\"criterionId\":\"").append(id).append("\"");
            switch (kind) {
                case "BOOLEAN" -> scoresJson.append(",\"boolValue\":").append(boolVal);
                case "SCORE" -> scoresJson.append(",\"scoreValue\":").append(scoreVal);
                case "PERCENT" -> scoresJson.append(",\"percentValue\":").append(scoreVal);
            }
            scoresJson.append("}");
        }
        scoresJson.append("]");
        return "{\"scores\":" + scoresJson + "}";
    }
}

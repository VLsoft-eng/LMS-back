package com.example.lms.bdd;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TICKET #9185: step definitions for peer_review_config.feature
 */
@RequiredArgsConstructor
public class PeerReviewConfigSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PeerReviewTestContext ctx;

    @Autowired
    private CommonSteps common;

    @Autowired
    private ScenarioSetupHelper setup;

    // ── Given ───────────────────────────────────────────────────────────────

    @Given("a class exists with a teacher and {int} enrolled students")
    public void classWithTeacherAndStudents(int studentCount) throws Exception {
        setup.createClassWithStudents(ctx, studentCount);
    }

    @Given("an assignment exists in that class")
    public void assignmentExists() throws Exception {
        setup.createAssignment(ctx);
    }

    @Given("the assignment has a rubric attached")
    public void assignmentHasRubric() throws Exception {
        setup.attachRubric(ctx);
    }

    @Given("the assignment has no rubric attached")
    public void assignmentHasNoRubric() {
        // Nothing to do — rubric is not attached by default
        ctx.rubricId = null;
    }

    @Given("peer review is already configured with reviewsPerStudent = {int}")
    public void peerReviewAlreadyConfigured(int count) throws Exception {
        mockMvc.perform(post("/api/v1/assignments/" + ctx.assignmentId + "/peer-review")
                .header("Authorization", "Bearer " + ctx.teacherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reviewsPerStudent\": " + count + "}"))
                .andExpect(status().isCreated());
    }

    // ── When ────────────────────────────────────────────────────────────────

    @When("the teacher configures peer review with reviewsPerStudent = {int}")
    public void teacherConfiguresPeerReview(int count) throws Exception {
        ctx.lastResponse = mockMvc.perform(
                post("/api/v1/assignments/" + ctx.assignmentId + "/peer-review")
                        .header("Authorization", "Bearer " + ctx.teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reviewsPerStudent\": " + count + "}"));
    }

    @When("a student tries to configure peer review with reviewsPerStudent = {int}")
    public void studentConfiguresPeerReview(int count) throws Exception {
        String studentToken = ctx.studentTokens.values().iterator().next();
        ctx.lastResponse = mockMvc.perform(
                post("/api/v1/assignments/" + ctx.assignmentId + "/peer-review")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reviewsPerStudent\": " + count + "}"));
    }

    @When("an unauthenticated user tries to configure peer review")
    public void unauthenticatedConfiguresPeerReview() throws Exception {
        ctx.lastResponse = mockMvc.perform(
                post("/api/v1/assignments/" + ctx.assignmentId + "/peer-review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reviewsPerStudent\": 1}"));
    }

    @When("the teacher retrieves peer review settings for the assignment")
    public void teacherGetSettings() throws Exception {
        ctx.lastResponse = mockMvc.perform(
                get("/api/v1/assignments/" + ctx.assignmentId + "/peer-review")
                        .header("Authorization", "Bearer " + ctx.teacherToken));
    }

    // ── Then ────────────────────────────────────────────────────────────────

    @Then("peer review settings should be created with reviewsPerStudent = {int}")
    public void settingsCreatedWithCount(int count) throws Exception {
        ctx.lastResponse
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reviewsPerStudent").value(count));
    }

    @Then("the existing settings should be updated with reviewsPerStudent = {int}")
    public void settingsUpdatedWithCount(int count) throws Exception {
        ctx.lastResponse
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reviewsPerStudent").value(count));
    }

    @Then("isEnabled should be true")
    public void isEnabledTrue() throws Exception {
        ctx.lastResponse.andExpect(jsonPath("$.isEnabled").value(true));
    }

    @Then("the response status should be {int}")
    public void responseStatus(int code) throws Exception {
        ctx.lastResponse.andExpect(status().is(code));
    }

    @Then("the error code should be {string}")
    public void errorCode(String code) throws Exception {
        ctx.lastResponse.andExpect(jsonPath("$.code").value(code));
    }

    @Then("the returned settings should have reviewsPerStudent = {int}")
    public void returnedSettingsHasCount(int count) throws Exception {
        ctx.lastResponse
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewsPerStudent").value(count));
    }
}

package com.example.lms.bdd;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.ResultActions;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * TICKET #9185: shared state within a single Cucumber scenario.
 * Singleton bean — reset() is called in @Before to clear state between scenarios.
 * Note: @ScenarioScope creates a CGLIB proxy that breaks direct field access,
 * so we use a singleton with manual reset instead.
 */
@Profile("cucumber")
@Component
public class PeerReviewTestContext {

    // IDs created during scenario
    public UUID classId;
    public UUID assignmentId;
    public UUID rubricId;
    public UUID teacherUserId;
    public String teacherToken;
    public String studentToken;
    public UUID studentUserId;

    // Named student tokens (Alice / Bob / Carol)
    public Map<String, String> studentTokens = new HashMap<>();
    public Map<String, UUID> studentIds = new HashMap<>();
    public Map<String, UUID> submissionIds = new HashMap<>();
    public Map<String, UUID> praIds = new HashMap<>();

    // Last HTTP response for assertion
    public ResultActions lastResponse;

    // Peer review settings
    public UUID praId;

    public void reset() {
        classId = null;
        assignmentId = null;
        rubricId = null;
        teacherUserId = null;
        teacherToken = null;
        studentToken = null;
        studentUserId = null;
        studentTokens = new HashMap<>();
        studentIds = new HashMap<>();
        submissionIds = new HashMap<>();
        praIds = new HashMap<>();
        lastResponse = null;
        praId = null;
    }
}

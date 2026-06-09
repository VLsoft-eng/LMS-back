package com.example.lms.bdd;

import io.cucumber.spring.ScenarioScope;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.ResultActions;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * TICKET #9185: shared state within a single Cucumber scenario.
 * ScenarioScope creates a new instance per scenario — no cross-scenario leakage.
 */
@Component
@ScenarioScope
public class PeerReviewTestContext {

    // IDs created during scenario
    public UUID classId;
    public UUID assignmentId;
    public UUID rubricId;
    public UUID teacherUserId;
    public String teacherToken;
    public String studentToken;        // "current" student's token
    public UUID studentUserId;

    // Named student tokens (Alice / Bob / Carol)
    public final Map<String, String> studentTokens = new HashMap<>();
    public final Map<String, UUID> studentIds = new HashMap<>();
    public final Map<String, UUID> submissionIds = new HashMap<>();
    public final Map<String, UUID> praIds = new HashMap<>();  // per-student PRA IDs

    // Last HTTP response for assertion
    public ResultActions lastResponse;

    // Peer review settings
    public UUID praId;  // single PRA for simple scenarios
}

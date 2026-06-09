Feature: Peer Assessment Submit
  As a student reviewer
  I want to submit an assessment of a peer's work
  Using the rubric criteria defined by the teacher

  Background:
    Given a class with a teacher and 3 students (Alice, Bob, Carol)
    And an assignment with a rubric containing 2 criteria (boolean + score)
    And all students have submitted their work
    And peer review is configured and distributed
    And Alice is assigned to review Bob's submission

  Scenario: Student successfully submits a peer assessment
    Given Alice is authenticated
    When Alice submits scores for all criteria of Bob's submission
    Then a peer assessment should be created
    And the peer review assignment status should be SUBMITTED
    And finalScoreNormalized should be between 0 and 100

  Scenario: Student cannot review their own submission
    Given Bob is authenticated
    When Bob tries to submit an assessment for his own submission
    Then the response status should be 403
    And the error code should be "PEER_SELF_REVIEW_FORBIDDEN"

  Scenario: Student cannot submit a peer assessment twice (duplicate)
    Given Alice is authenticated
    And Alice has already submitted an assessment for Bob's submission
    When Alice tries to submit another assessment for the same assignment
    Then the response status should be 409
    And the error code should be "PEER_ASSESSMENT_ALREADY_EXISTS"

  Scenario: Student cannot assess a submission not assigned to them
    Given Carol is authenticated
    When Carol tries to submit an assessment for an assignment not in her review queue
    Then the response status should be 403

  Scenario: Assessment scores must match the rubric criteria count
    Given Alice is authenticated
    When Alice submits scores for only 1 of the 2 criteria
    Then the response status should be 400

  Scenario: Score value must be within criterion scale
    Given Alice is authenticated
    And the rubric has a score criterion with maxPoints = 10
    When Alice submits a score value of 999 for that criterion
    Then the response status should be 400

  Scenario: Student can update a previously submitted peer assessment
    Given Alice is authenticated
    And Alice has already submitted an assessment for Bob's submission
    When Alice updates her assessment with new scores
    Then the updated peer assessment should be returned
    And the status remains SUBMITTED

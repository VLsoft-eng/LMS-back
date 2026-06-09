Feature: Peer Review Distribution
  As a teacher
  I want to distribute submissions among student reviewers
  So that each student reviews the configured number of peers' work

  Background:
    Given a class exists with a teacher and 4 enrolled students
    And an assignment exists in that class with a rubric attached
    And peer review is configured with reviewsPerStudent = 1
    And all 4 students have submitted their work

  Scenario: Teacher distributes reviewers successfully
    When the teacher triggers reviewer distribution
    Then 4 peer review assignments should be created
    And each student should have exactly 1 assigned submission to review
    And no student should be assigned their own submission

  Scenario: Distribution is idempotent (calling twice does not duplicate)
    When the teacher triggers reviewer distribution
    And the teacher triggers reviewer distribution again
    Then there should still be exactly 4 peer review assignments

  Scenario: All assigned submissions belong to the correct assignment
    When the teacher triggers reviewer distribution
    Then all peer review assignments should reference submissions of this assignment

  Scenario: Cannot distribute when fewer than 2 submissions exist
    Given only 1 student has submitted their work
    When the teacher tries to trigger reviewer distribution
    Then the response status should be 409
    And the error code should be "PEER_REVIEW_NOT_ENOUGH_SUBMISSIONS"

  Scenario: Student cannot trigger distribution
    When a student tries to trigger reviewer distribution
    Then the response status should be 403

  Scenario: ReviewsPerStudent exceeds available peers (capped at submissions - 1)
    Given peer review is configured with reviewsPerStudent = 10
    When the teacher triggers reviewer distribution
    Then each student should have at most 3 assigned submissions to review

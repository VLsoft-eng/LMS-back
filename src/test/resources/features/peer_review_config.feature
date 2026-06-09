Feature: Peer Review Configuration
  As a teacher
  I want to configure peer review for an assignment
  So that students can assess each other's work using rubric criteria

  Background:
    Given a class exists with a teacher and 3 enrolled students
    And an assignment exists in that class

  Scenario: Teacher successfully enables peer review with a rubric attached
    Given the assignment has a rubric attached
    When the teacher configures peer review with reviewsPerStudent = 1
    Then peer review settings should be created with reviewsPerStudent = 1
    And isEnabled should be true

  Scenario: Teacher updates peer review settings (idempotent configure)
    Given the assignment has a rubric attached
    And peer review is already configured with reviewsPerStudent = 1
    When the teacher configures peer review with reviewsPerStudent = 2
    Then the existing settings should be updated with reviewsPerStudent = 2

  Scenario: Cannot enable peer review without a rubric
    Given the assignment has no rubric attached
    When the teacher tries to configure peer review with reviewsPerStudent = 1
    Then the response status should be 409
    And the error code should be "PEER_REVIEW_NO_RUBRIC"

  Scenario: Student cannot configure peer review
    Given the assignment has a rubric attached
    When a student tries to configure peer review with reviewsPerStudent = 1
    Then the response status should be 403

  Scenario: Non-member cannot configure peer review
    Given the assignment has a rubric attached
    When an unauthenticated user tries to configure peer review
    Then the response status should be 401

  Scenario: Teacher can retrieve peer review settings
    Given the assignment has a rubric attached
    And peer review is already configured with reviewsPerStudent = 2
    When the teacher retrieves peer review settings for the assignment
    Then the returned settings should have reviewsPerStudent = 2
    And isEnabled should be true

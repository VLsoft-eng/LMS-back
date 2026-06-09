Feature: Peer Assessment View
  As a student or teacher
  I want to view peer assessment results
  So that feedback from peers is visible to the right parties

  Background:
    Given a class with a teacher and 3 students (Alice, Bob, Carol)
    And an assignment with a rubric containing 2 criteria
    And all students have submitted and peer reviews have been distributed
    And Alice has submitted a peer assessment for Bob's submission

  Scenario: Student retrieves their assigned review queue
    Given Alice is authenticated
    When Alice requests her assigned reviews for the assignment
    Then the response should contain her assigned submissions
    And the status of Bob's assignment should be SUBMITTED

  Scenario: Student sees received peer assessments for their own submission
    Given Bob is authenticated
    When Bob requests peer assessments received for his submission
    Then the response should contain Alice's assessment (anonymized)
    And the response should NOT contain Alice's identity (reviewerId/reviewerName)

  Scenario: Student without received assessments gets empty list
    Given Carol is authenticated
    And no one has reviewed Carol's submission yet
    When Carol requests received peer assessments
    Then the response should contain an empty assessments list

  Scenario: Teacher sees aggregated results for all submissions
    Given the teacher is authenticated
    When the teacher requests peer review results for the assignment
    Then the response should contain entries for all submissions that received assessments
    And each entry should contain averageScore and assessmentCount

  Scenario: Student cannot access another student's review queue
    Given Bob is authenticated
    When Bob tries to access Alice's assigned review queue
    Then the response status should be 403

  Scenario: Teacher can retrieve all peer review assignments
    Given the teacher is authenticated
    When the teacher requests all peer review assignments for the assignment
    Then all peer review assignments should be returned
    And each assignment should include reviewer and submission info

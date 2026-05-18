-- TICKET-BE-34: assessments + criterion_scores

CREATE TABLE assessments (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rubric_id              UUID         NOT NULL REFERENCES rubrics (id) ON DELETE RESTRICT,
    assignment_id          UUID         NOT NULL REFERENCES assignments (id) ON DELETE CASCADE,
    submission_id          UUID         REFERENCES submissions (id) ON DELETE CASCADE,
    team_grade_id          UUID         REFERENCES team_grades (id) ON DELETE CASCADE,
    primary_sum            NUMERIC(10, 2) NOT NULL,
    bonus_multiplier       NUMERIC(6, 4)  NOT NULL,
    final_score            NUMERIC(10, 2) NOT NULL,
    final_score_normalized SMALLINT     NOT NULL CHECK (final_score_normalized BETWEEN 0 AND 100),
    graded_by              UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    graded_at              TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT assess_target_xor CHECK (
        (submission_id IS NOT NULL AND team_grade_id IS NULL) OR
        (submission_id IS NULL AND team_grade_id IS NOT NULL)
    )
);

CREATE UNIQUE INDEX uq_assess_submission ON assessments (submission_id) WHERE submission_id IS NOT NULL;
CREATE UNIQUE INDEX uq_assess_team_grade ON assessments (team_grade_id) WHERE team_grade_id IS NOT NULL;
CREATE INDEX idx_assess_rubric ON assessments (rubric_id);
CREATE INDEX idx_assess_assignment ON assessments (assignment_id);

CREATE TABLE criterion_scores (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessment_id   UUID         NOT NULL REFERENCES assessments (id) ON DELETE CASCADE,
    criterion_id    UUID         NOT NULL REFERENCES criteria (id) ON DELETE RESTRICT,
    bool_value      BOOLEAN,
    percent_value   NUMERIC(5, 2),
    score_value     NUMERIC(10, 2),
    computed_points NUMERIC(10, 4) NOT NULL,
    comment         VARCHAR(500),
    UNIQUE (assessment_id, criterion_id)
);

CREATE INDEX idx_cscores_assessment ON criterion_scores (assessment_id);

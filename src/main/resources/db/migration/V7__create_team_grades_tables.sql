-- TICKET-BE-19: team_grades + individual_grade_adjustments

CREATE TABLE team_grades (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id       UUID      NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    assignment_id UUID      NOT NULL REFERENCES assignments (id) ON DELETE CASCADE,
    grade         SMALLINT  NOT NULL CHECK (grade BETWEEN 0 AND 100),
    comment       TEXT,
    graded_by     UUID      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    graded_at     TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (team_id, assignment_id)
);

CREATE TABLE individual_grade_adjustments (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_grade_id  UUID      NOT NULL REFERENCES team_grades (id) ON DELETE CASCADE,
    student_id     UUID      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    adjustment     SMALLINT  NOT NULL DEFAULT 0 CHECK (adjustment BETWEEN -50 AND 50),
    final_grade    SMALLINT  NOT NULL CHECK (final_grade BETWEEN 0 AND 100),
    comment        TEXT,
    graded_by      UUID      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    graded_at      TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (team_grade_id, student_id)
);

CREATE INDEX idx_team_grades_team_id ON team_grades (team_id);
CREATE INDEX idx_team_grades_assignment_id ON team_grades (assignment_id);
CREATE INDEX idx_individual_adjustments_team_grade_id ON individual_grade_adjustments (team_grade_id);
CREATE INDEX idx_individual_adjustments_student_id ON individual_grade_adjustments (student_id);

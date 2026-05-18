-- TICKET-BE-34: rubric snapshots (привязка к assignment) + assignments.rubric_id

CREATE TABLE rubrics (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assignment_id      UUID         NOT NULL UNIQUE REFERENCES assignments (id) ON DELETE CASCADE,
    source_template_id UUID         REFERENCES rubric_templates (id) ON DELETE SET NULL,
    name               VARCHAR(200) NOT NULL,
    description        VARCHAR(2000),
    total_max_points   NUMERIC(10, 2) NOT NULL,
    allow_overcap      BOOLEAN      NOT NULL,
    frozen_at          TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE criteria (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rubric_id       UUID         NOT NULL REFERENCES rubrics (id) ON DELETE CASCADE,
    ordinal         INT          NOT NULL,
    title           VARCHAR(200) NOT NULL,
    description     VARCHAR(2000),
    kind            VARCHAR(16)  NOT NULL CHECK (kind IN ('BOOLEAN', 'PERCENT', 'SCORE')),
    role            VARCHAR(16)  NOT NULL CHECK (role IN ('PRIMARY', 'BONUS')),
    max_points      NUMERIC(10, 2),
    max_coefficient NUMERIC(6, 4),
    score_min       NUMERIC(10, 2),
    score_max       NUMERIC(10, 2),
    CONSTRAINT crit_primary_has_points CHECK (
        (role = 'PRIMARY' AND max_points IS NOT NULL AND max_coefficient IS NULL) OR
        (role = 'BONUS' AND max_coefficient IS NOT NULL AND max_points IS NULL)
    ),
    CONSTRAINT crit_score_has_range CHECK (
        kind <> 'SCORE' OR (score_min IS NOT NULL AND score_max IS NOT NULL AND score_max > score_min)
    ),
    UNIQUE (rubric_id, ordinal)
);

CREATE INDEX idx_criteria_rubric ON criteria (rubric_id);

ALTER TABLE assignments ADD COLUMN rubric_id UUID REFERENCES rubrics (id) ON DELETE SET NULL;
CREATE INDEX idx_assignments_rubric ON assignments (rubric_id);

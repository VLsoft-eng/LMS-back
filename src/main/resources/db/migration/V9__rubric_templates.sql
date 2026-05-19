-- TICKET-BE-34: rubric templates (на уровне класса)

CREATE TABLE rubric_templates (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    class_id         UUID         NOT NULL REFERENCES classes (id) ON DELETE CASCADE,
    name             VARCHAR(200) NOT NULL,
    description      VARCHAR(2000),
    total_max_points NUMERIC(10, 2) NOT NULL CHECK (total_max_points > 0 AND total_max_points <= 1000),
    allow_overcap    BOOLEAN      NOT NULL DEFAULT false,
    created_by       UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at       TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_rubric_templates_class ON rubric_templates (class_id);

CREATE TABLE criterion_templates (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rubric_template_id UUID         NOT NULL REFERENCES rubric_templates (id) ON DELETE CASCADE,
    ordinal            INT          NOT NULL CHECK (ordinal >= 0),
    title              VARCHAR(200) NOT NULL,
    description        VARCHAR(2000),
    kind               VARCHAR(16)  NOT NULL CHECK (kind IN ('BOOLEAN', 'PERCENT', 'SCORE')),
    role               VARCHAR(16)  NOT NULL CHECK (role IN ('PRIMARY', 'BONUS')),
    max_points         NUMERIC(10, 2),
    max_coefficient    NUMERIC(6, 4),
    score_min          NUMERIC(10, 2),
    score_max          NUMERIC(10, 2),
    CONSTRAINT crit_tpl_primary_has_points CHECK (
        (role = 'PRIMARY' AND max_points IS NOT NULL AND max_coefficient IS NULL) OR
        (role = 'BONUS' AND max_coefficient IS NOT NULL AND max_points IS NULL)
    ),
    CONSTRAINT crit_tpl_score_has_range CHECK (
        kind <> 'SCORE' OR (score_min IS NOT NULL AND score_max IS NOT NULL AND score_max > score_min)
    ),
    CONSTRAINT crit_tpl_bonus_coef_range CHECK (
        role <> 'BONUS' OR (max_coefficient > 1.0000 AND max_coefficient <= 2.0000)
    ),
    UNIQUE (rubric_template_id, ordinal)
);

CREATE INDEX idx_crit_tpl_rubric ON criterion_templates (rubric_template_id);

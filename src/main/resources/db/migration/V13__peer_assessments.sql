-- TICKET #9179: P2P: Flyway V13 — peer_assessments + peer_criterion_scores

CREATE TABLE peer_assessments (
    id                          UUID            NOT NULL DEFAULT gen_random_uuid(),
    peer_review_assignment_id   UUID            NOT NULL,
    rubric_id                   UUID            NOT NULL,
    primary_sum                 NUMERIC(10, 2)  NOT NULL,
    bonus_multiplier            NUMERIC(6, 4)   NOT NULL,
    final_score                 NUMERIC(10, 2)  NOT NULL,
    final_score_normalized      SMALLINT        NOT NULL,
    submitted_at                TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT pk_peer_assessments PRIMARY KEY (id),
    CONSTRAINT uq_peer_assessment_assignment UNIQUE (peer_review_assignment_id),
    CONSTRAINT fk_peer_assessment_pra
        FOREIGN KEY (peer_review_assignment_id)
        REFERENCES peer_review_assignments(id) ON DELETE CASCADE,
    CONSTRAINT fk_peer_assessment_rubric
        FOREIGN KEY (rubric_id) REFERENCES rubrics(id) ON DELETE RESTRICT,
    CONSTRAINT chk_peer_final_score_normalized
        CHECK (final_score_normalized BETWEEN 0 AND 100)
);

CREATE INDEX idx_peer_assessments_pra ON peer_assessments(peer_review_assignment_id);

-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE peer_criterion_scores (
    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    peer_assessment_id  UUID            NOT NULL,
    criterion_id        UUID            NOT NULL,
    bool_value          BOOLEAN,
    percent_value       NUMERIC(5, 2),
    score_value         NUMERIC(10, 2),
    computed_points     NUMERIC(10, 4)  NOT NULL DEFAULT 0,
    comment             VARCHAR(500),

    CONSTRAINT pk_peer_criterion_scores PRIMARY KEY (id),
    CONSTRAINT uq_peer_criterion_score UNIQUE (peer_assessment_id, criterion_id),
    CONSTRAINT fk_peer_criterion_scores_assessment
        FOREIGN KEY (peer_assessment_id) REFERENCES peer_assessments(id) ON DELETE CASCADE,
    CONSTRAINT fk_peer_criterion_scores_criterion
        FOREIGN KEY (criterion_id) REFERENCES criteria(id) ON DELETE RESTRICT
);

CREATE INDEX idx_peer_criterion_scores_assessment ON peer_criterion_scores(peer_assessment_id);

-- TICKET #9179: P2P: Flyway V12 — peer_review_settings + peer_review_assignments

CREATE TABLE peer_review_settings (
    id               UUID         NOT NULL DEFAULT gen_random_uuid(),
    assignment_id    UUID         NOT NULL,
    reviews_per_student INT       NOT NULL DEFAULT 1,
    is_enabled       BOOLEAN      NOT NULL DEFAULT FALSE,
    due_date         TIMESTAMPTZ,
    created_by       UUID         NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_peer_review_settings PRIMARY KEY (id),
    CONSTRAINT uq_peer_review_settings_assignment UNIQUE (assignment_id),
    CONSTRAINT fk_peer_review_settings_assignment
        FOREIGN KEY (assignment_id) REFERENCES assignments(id) ON DELETE CASCADE,
    CONSTRAINT chk_reviews_per_student CHECK (reviews_per_student >= 1)
);

CREATE INDEX idx_peer_review_settings_assignment ON peer_review_settings(assignment_id);

-- ─────────────────────────────────────────────────────────────────────────────

CREATE TYPE peer_review_status AS ENUM ('PENDING', 'SUBMITTED');

CREATE TABLE peer_review_assignments (
    id              UUID                NOT NULL DEFAULT gen_random_uuid(),
    assignment_id   UUID                NOT NULL,
    reviewer_id     UUID                NOT NULL,
    submission_id   UUID                NOT NULL,
    status          peer_review_status  NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMPTZ         NOT NULL DEFAULT now(),

    CONSTRAINT pk_peer_review_assignments PRIMARY KEY (id),
    CONSTRAINT uq_peer_review_assignment_pair
        UNIQUE (assignment_id, reviewer_id, submission_id),
    CONSTRAINT fk_peer_review_assignments_assignment
        FOREIGN KEY (assignment_id) REFERENCES assignments(id) ON DELETE CASCADE,
    CONSTRAINT fk_peer_review_assignments_submission
        FOREIGN KEY (submission_id) REFERENCES submissions(id) ON DELETE CASCADE
    -- self-review guard enforced at service layer (PostgreSQL does not allow subqueries in CHECK)
);

CREATE INDEX idx_peer_review_assignments_reviewer  ON peer_review_assignments(reviewer_id);
CREATE INDEX idx_peer_review_assignments_assignment ON peer_review_assignments(assignment_id);
CREATE INDEX idx_peer_review_assignments_submission  ON peer_review_assignments(submission_id);

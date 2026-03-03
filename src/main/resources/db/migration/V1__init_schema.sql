-- LMS Backend: initial schema (TICKET-BE-02)
-- All PKs UUID, gen_random_uuid()

CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    avatar_url    VARCHAR(500),
    date_of_birth DATE,
    created_at    TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE classes (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(255) NOT NULL,
    code       CHAR(8)      NOT NULL UNIQUE,
    owner_id   UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE class_members (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    class_id  UUID      NOT NULL REFERENCES classes (id) ON DELETE CASCADE,
    user_id   UUID      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role      VARCHAR(20) NOT NULL CHECK (role IN ('OWNER', 'TEACHER', 'STUDENT')),
    joined_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (class_id, user_id)
);

CREATE TABLE assignments (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    class_id    UUID      NOT NULL REFERENCES classes (id) ON DELETE CASCADE,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    created_by  UUID      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE submissions (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assignment_id UUID      NOT NULL REFERENCES assignments (id) ON DELETE CASCADE,
    student_id    UUID      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    answer_text   TEXT,
    file_path     VARCHAR(500),
    grade         SMALLINT CHECK (grade IS NULL OR (grade >= 0 AND grade <= 100)),
    submitted_at  TIMESTAMP NOT NULL DEFAULT now(),
    graded_at     TIMESTAMP
);

CREATE TABLE comments (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assignment_id UUID      NOT NULL REFERENCES assignments (id) ON DELETE CASCADE,
    author_id     UUID      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    text          TEXT      NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT now()
);

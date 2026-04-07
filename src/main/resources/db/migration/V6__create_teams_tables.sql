-- TICKET-BE-18: teams + team_members

CREATE TABLE teams (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    class_id      UUID         NOT NULL REFERENCES classes (id) ON DELETE CASCADE,
    assignment_id UUID                  REFERENCES assignments (id) ON DELETE CASCADE,
    name          VARCHAR(100) NOT NULL,
    created_by    UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at    TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE team_members (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id   UUID      NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    user_id   UUID      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    is_leader BOOLEAN   NOT NULL DEFAULT false,
    joined_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (team_id, user_id)
);

CREATE INDEX idx_teams_class_id ON teams (class_id);
CREATE INDEX idx_teams_assignment_id ON teams (assignment_id);
CREATE INDEX idx_team_members_team_id ON team_members (team_id);
CREATE INDEX idx_team_members_user_id ON team_members (user_id);

-- TICKET-BE-20: assignment type + is_team_based

ALTER TABLE assignments ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'STANDARD';
ALTER TABLE assignments ADD COLUMN is_team_based BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE assignments ADD CONSTRAINT chk_assignment_type CHECK (type IN ('STANDARD', 'QUICK'));

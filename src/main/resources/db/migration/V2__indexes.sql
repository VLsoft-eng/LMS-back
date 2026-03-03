-- LMS Backend: indexes on foreign keys (TICKET-BE-02)

CREATE INDEX idx_classes_owner_id ON classes (owner_id);

CREATE INDEX idx_class_members_class_id ON class_members (class_id);
CREATE INDEX idx_class_members_user_id ON class_members (user_id);

CREATE INDEX idx_assignments_class_id ON assignments (class_id);
CREATE INDEX idx_assignments_created_by ON assignments (created_by);

CREATE INDEX idx_submissions_assignment_id ON submissions (assignment_id);
CREATE INDEX idx_submissions_student_id ON submissions (student_id);

CREATE INDEX idx_comments_assignment_id ON comments (assignment_id);
CREATE INDEX idx_comments_author_id ON comments (author_id);

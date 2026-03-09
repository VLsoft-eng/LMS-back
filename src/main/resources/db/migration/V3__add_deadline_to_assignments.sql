-- TICKET-BE-10: Add deadline column to assignments
ALTER TABLE assignments ADD COLUMN deadline TIMESTAMP;

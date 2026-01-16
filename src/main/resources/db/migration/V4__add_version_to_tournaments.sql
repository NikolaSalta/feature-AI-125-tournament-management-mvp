-- Add version column for optimistic locking to tournaments table

ALTER TABLE tournaments
ADD COLUMN version BIGINT DEFAULT 0;

-- Update existing rows to have version = 0
UPDATE tournaments SET version = 0 WHERE version IS NULL;

-- Make version NOT NULL
ALTER TABLE tournaments
ALTER COLUMN version SET NOT NULL;

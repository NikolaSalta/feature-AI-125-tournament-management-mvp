-- Flyway migration: Add winner fields to tournaments
-- Version: 8
-- Description: Add winner_id and completed_at fields for tournament completion

-- Add winner_id column
ALTER TABLE tournaments 
ADD COLUMN winner_id BIGINT;

-- Add completed_at column
ALTER TABLE tournaments 
ADD COLUMN completed_at TIMESTAMP;

-- Add foreign key constraint (winner references tournament_players)
-- Note: We use a simple column reference, not FK to tournament_players,
-- because tournament_players may be deleted but we want to keep winner info

-- Add index for winner lookups
CREATE INDEX idx_tournament_winner ON tournaments(winner_id);

-- Comments
COMMENT ON COLUMN tournaments.winner_id IS 'ID победителя турнира (TournamentPlayer ID)';
COMMENT ON COLUMN tournaments.completed_at IS 'Дата и время завершения турнира';

-- Flyway migration: Create tournament_players table
-- Version: 6
-- Description: Table for tournament participants

-- =====================
-- Tournament Players Table
-- =====================
CREATE TABLE tournament_players (
    id BIGSERIAL PRIMARY KEY,
    tournament_id BIGINT NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rating_at_registration INTEGER,
    score DECIMAL(4,1) NOT NULL DEFAULT 0.0,
    games_played INTEGER NOT NULL DEFAULT 0,
    wins INTEGER NOT NULL DEFAULT 0,
    draws INTEGER NOT NULL DEFAULT 0,
    losses INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'REGISTERED',
    final_rank INTEGER,
    registered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT uk_tournament_player UNIQUE (tournament_id, user_id),
    CONSTRAINT chk_tp_score CHECK (score >= 0),
    CONSTRAINT chk_tp_games CHECK (games_played >= 0),
    CONSTRAINT chk_tp_wins CHECK (wins >= 0),
    CONSTRAINT chk_tp_draws CHECK (draws >= 0),
    CONSTRAINT chk_tp_losses CHECK (losses >= 0),
    CONSTRAINT chk_tp_status CHECK (status IN ('REGISTERED', 'ACTIVE', 'WITHDRAWN', 'DISQUALIFIED'))
);

-- Indexes
CREATE INDEX idx_tp_tournament ON tournament_players(tournament_id);
CREATE INDEX idx_tp_user ON tournament_players(user_id);
CREATE INDEX idx_tp_score ON tournament_players(score DESC);
CREATE INDEX idx_tp_status ON tournament_players(status);

-- Comments
COMMENT ON TABLE tournament_players IS 'Участники турниров';
COMMENT ON COLUMN tournament_players.rating_at_registration IS 'Рейтинг на момент регистрации';
COMMENT ON COLUMN tournament_players.score IS 'Набранные очки (победа=1, ничья=0.5)';
COMMENT ON COLUMN tournament_players.status IS 'REGISTERED, ACTIVE, WITHDRAWN, DISQUALIFIED';
COMMENT ON COLUMN tournament_players.final_rank IS 'Итоговое место (после завершения турнира)';

-- Trigger for updated_at
CREATE TRIGGER update_tournament_players_updated_at
    BEFORE UPDATE ON tournament_players
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Flyway migration: Create games table
-- Version: 7
-- Description: Table for tournament games/matches

-- =====================
-- Games Table
-- =====================
CREATE TABLE games (
    id BIGSERIAL PRIMARY KEY,
    tournament_id BIGINT NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    white_player_id BIGINT NOT NULL REFERENCES tournament_players(id) ON DELETE CASCADE,
    black_player_id BIGINT NOT NULL REFERENCES tournament_players(id) ON DELETE CASCADE,
    round_number INTEGER NOT NULL,
    board_number INTEGER,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    result VARCHAR(20),
    white_score DECIMAL(2,1),
    black_score DECIMAL(2,1),
    scheduled_at TIMESTAMP,
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    pgn TEXT,
    notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT chk_game_round CHECK (round_number >= 1),
    CONSTRAINT chk_game_board CHECK (board_number IS NULL OR board_number >= 1),
    CONSTRAINT chk_game_status CHECK (status IN ('SCHEDULED', 'IN_PROGRESS', 'FINISHED', 'CANCELLED')),
    CONSTRAINT chk_game_result CHECK (result IS NULL OR result IN ('WHITE_WINS', 'BLACK_WINS', 'DRAW', 'WHITE_FORFEIT', 'BLACK_FORFEIT', 'NOT_PLAYED')),
    CONSTRAINT chk_game_white_score CHECK (white_score IS NULL OR white_score IN (0.0, 0.5, 1.0)),
    CONSTRAINT chk_game_black_score CHECK (black_score IS NULL OR black_score IN (0.0, 0.5, 1.0)),
    CONSTRAINT chk_game_different_players CHECK (white_player_id != black_player_id)
);

-- Indexes
CREATE INDEX idx_game_tournament ON games(tournament_id);
CREATE INDEX idx_game_white ON games(white_player_id);
CREATE INDEX idx_game_black ON games(black_player_id);
CREATE INDEX idx_game_round ON games(tournament_id, round_number);
CREATE INDEX idx_game_status ON games(status);

-- Comments
COMMENT ON TABLE games IS 'Партии турнира';
COMMENT ON COLUMN games.round_number IS 'Номер раунда/тура';
COMMENT ON COLUMN games.board_number IS 'Номер доски/столика';
COMMENT ON COLUMN games.status IS 'SCHEDULED, IN_PROGRESS, FINISHED, CANCELLED';
COMMENT ON COLUMN games.result IS 'WHITE_WINS, BLACK_WINS, DRAW, WHITE_FORFEIT, BLACK_FORFEIT, NOT_PLAYED';
COMMENT ON COLUMN games.white_score IS 'Очки белых: 1.0=победа, 0.5=ничья, 0.0=поражение';
COMMENT ON COLUMN games.black_score IS 'Очки чёрных: 1.0=победа, 0.5=ничья, 0.0=поражение';
COMMENT ON COLUMN games.pgn IS 'PGN-нотация партии';

-- Trigger for updated_at
CREATE TRIGGER update_games_updated_at
    BEFORE UPDATE ON games
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

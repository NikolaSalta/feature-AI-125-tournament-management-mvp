-- Flyway migration: Create tournament_winners table
-- Version: 9
-- Description: Table for multiple tournament winners (supports children tournaments where everyone wins)

-- =====================
-- Tournament Winners Table
-- =====================
CREATE TABLE tournament_winners (
    id BIGSERIAL PRIMARY KEY,
    tournament_id BIGINT NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    player_id BIGINT NOT NULL REFERENCES tournament_players(id) ON DELETE CASCADE,
    place INTEGER,
    award_type VARCHAR(30) NOT NULL DEFAULT 'PLACE',
    award_title VARCHAR(200),
    description VARCHAR(500),
    prize_amount INTEGER,
    awarded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT chk_tw_place CHECK (place IS NULL OR place >= 1),
    CONSTRAINT chk_tw_award_type CHECK (award_type IN ('PLACE', 'SPECIAL', 'PARTICIPATION')),
    CONSTRAINT chk_tw_prize CHECK (prize_amount IS NULL OR prize_amount >= 0)
);

-- Indexes
CREATE INDEX idx_tw_tournament ON tournament_winners(tournament_id);
CREATE INDEX idx_tw_player ON tournament_winners(player_id);
CREATE INDEX idx_tw_place ON tournament_winners(tournament_id, place);
CREATE INDEX idx_tw_award_type ON tournament_winners(tournament_id, award_type);

-- Comments
COMMENT ON TABLE tournament_winners IS 'Победители турнира (поддержка множественных победителей)';
COMMENT ON COLUMN tournament_winners.place IS 'Место (1, 2, 3... или null для специальных призов)';
COMMENT ON COLUMN tournament_winners.award_type IS 'PLACE=призовое место, SPECIAL=спецприз, PARTICIPATION=награда участника';
COMMENT ON COLUMN tournament_winners.award_title IS 'Название награды для специальных призов';
COMMENT ON COLUMN tournament_winners.prize_amount IS 'Призовая сумма (если есть)';

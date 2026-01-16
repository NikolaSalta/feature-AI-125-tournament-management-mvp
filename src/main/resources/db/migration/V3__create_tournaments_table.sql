-- Flyway migration: Create tournaments table
-- Version: 3
-- Description: Creates tournaments table for chess tournament management

-- =====================
-- Tournaments Table
-- =====================
CREATE TABLE tournaments (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(2000),
    format VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    organizer_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP,
    max_participants INTEGER,
    current_participants INTEGER NOT NULL DEFAULT 0,
    time_control_minutes INTEGER,
    time_increment_seconds INTEGER,
    min_rating INTEGER,
    max_rating INTEGER,
    prize_pool INTEGER,
    entry_fee INTEGER,
    is_public BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT chk_participants CHECK (current_participants >= 0),
    CONSTRAINT chk_max_participants CHECK (max_participants IS NULL OR max_participants >= 2),
    CONSTRAINT chk_dates CHECK (end_date IS NULL OR end_date >= start_date),
    CONSTRAINT chk_rating_range CHECK (
        (min_rating IS NULL OR min_rating >= 0) AND
        (max_rating IS NULL OR max_rating <= 3500) AND
        (min_rating IS NULL OR max_rating IS NULL OR min_rating <= max_rating)
    ),
    CONSTRAINT chk_time_control CHECK (time_control_minutes IS NULL OR time_control_minutes >= 1),
    CONSTRAINT chk_time_increment CHECK (time_increment_seconds IS NULL OR time_increment_seconds >= 0),
    CONSTRAINT chk_prize_pool CHECK (prize_pool IS NULL OR prize_pool >= 0),
    CONSTRAINT chk_entry_fee CHECK (entry_fee IS NULL OR entry_fee >= 0)
);

-- Indexes
CREATE INDEX idx_tournament_status ON tournaments(status);
CREATE INDEX idx_tournament_organizer ON tournaments(organizer_id);
CREATE INDEX idx_tournament_start_date ON tournaments(start_date);
CREATE INDEX idx_tournament_public ON tournaments(is_public);
CREATE INDEX idx_tournament_format ON tournaments(format);

-- Comments
COMMENT ON TABLE tournaments IS 'Шахматные турниры';
COMMENT ON COLUMN tournaments.name IS 'Название турнира';
COMMENT ON COLUMN tournaments.description IS 'Описание турнира';
COMMENT ON COLUMN tournaments.format IS 'Формат: ROUND_ROBIN, SWISS, KNOCKOUT, DOUBLE_ROUND_ROBIN';
COMMENT ON COLUMN tournaments.status IS 'Статус: DRAFT, REGISTRATION_OPEN, REGISTRATION_CLOSED, IN_PROGRESS, COMPLETED, CANCELLED';
COMMENT ON COLUMN tournaments.organizer_id IS 'ID организатора (пользователь)';
COMMENT ON COLUMN tournaments.start_date IS 'Дата и время начала турнира';
COMMENT ON COLUMN tournaments.end_date IS 'Дата и время окончания турнира';
COMMENT ON COLUMN tournaments.max_participants IS 'Максимальное количество участников';
COMMENT ON COLUMN tournaments.current_participants IS 'Текущее количество участников';
COMMENT ON COLUMN tournaments.time_control_minutes IS 'Контроль времени (минуты на партию)';
COMMENT ON COLUMN tournaments.time_increment_seconds IS 'Добавка времени (секунды за ход)';
COMMENT ON COLUMN tournaments.min_rating IS 'Минимальный рейтинг для участия';
COMMENT ON COLUMN tournaments.max_rating IS 'Максимальный рейтинг для участия';
COMMENT ON COLUMN tournaments.prize_pool IS 'Призовой фонд';
COMMENT ON COLUMN tournaments.entry_fee IS 'Взнос за участие';
COMMENT ON COLUMN tournaments.is_public IS 'Публичный ли турнир';

-- =====================================================
-- Триггер для автообновления updated_at
-- =====================================================
CREATE TRIGGER update_tournaments_updated_at
    BEFORE UPDATE ON tournaments
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- SEED DATA перенесены в afterMigrate__seed_dev.sql
-- Загружаются только при FLYWAY_PLACEHOLDERS_SEED_DATA=true
-- =====================================================







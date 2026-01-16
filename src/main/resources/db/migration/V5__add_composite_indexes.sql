-- =====================================================
-- Flyway migration: Add composite indexes
-- Version: 5
-- Description: Составные индексы для оптимизации запросов
-- =====================================================

-- =====================================================
-- Users Table - Composite Indexes
-- =====================================================

-- Индекс для поиска активных пользователей по email
-- Используется при аутентификации
CREATE INDEX IF NOT EXISTS idx_users_email_enabled 
    ON users(email, enabled) 
    WHERE enabled = true;

-- Индекс для поиска активных пользователей по username
-- Используется при аутентификации
CREATE INDEX IF NOT EXISTS idx_users_username_enabled 
    ON users(username, enabled) 
    WHERE enabled = true;

-- Частичный индекс для незаблокированных аккаунтов
CREATE INDEX IF NOT EXISTS idx_users_active_accounts 
    ON users(id, username, email) 
    WHERE enabled = true 
    AND account_non_locked = true 
    AND account_non_expired = true;

COMMENT ON INDEX idx_users_email_enabled IS 'Оптимизация аутентификации по email';
COMMENT ON INDEX idx_users_username_enabled IS 'Оптимизация аутентификации по username';
COMMENT ON INDEX idx_users_active_accounts IS 'Быстрый поиск активных аккаунтов';

-- =====================================================
-- Tournaments Table - Composite Indexes
-- =====================================================

-- Индекс для публичных турниров по статусу
-- Используется на главной странице и в поиске
CREATE INDEX IF NOT EXISTS idx_tournaments_public_status 
    ON tournaments(status, start_date) 
    WHERE is_public = true;

-- Индекс для поиска турниров организатора по статусу
-- Используется в личном кабинете организатора
CREATE INDEX IF NOT EXISTS idx_tournaments_organizer_status 
    ON tournaments(organizer_id, status);

-- Индекс для фильтрации по формату и статусу
-- Используется в расширенном поиске
CREATE INDEX IF NOT EXISTS idx_tournaments_format_status 
    ON tournaments(format, status);

-- Индекс для поиска предстоящих публичных турниров
-- Основной запрос на главной странице
CREATE INDEX IF NOT EXISTS idx_tournaments_upcoming_public 
    ON tournaments(start_date, status) 
    WHERE is_public = true 
    AND status IN ('DRAFT', 'REGISTRATION_OPEN', 'REGISTRATION_CLOSED');

-- Индекс для поиска по диапазону рейтинга
-- Используется при фильтрации турниров по рейтингу игрока
CREATE INDEX IF NOT EXISTS idx_tournaments_rating_range 
    ON tournaments(min_rating, max_rating) 
    WHERE min_rating IS NOT NULL OR max_rating IS NOT NULL;

-- Индекс для турниров с доступными слотами
-- Используется при регистрации участников
CREATE INDEX IF NOT EXISTS idx_tournaments_available_slots 
    ON tournaments(id, max_participants, current_participants) 
    WHERE status = 'REGISTRATION_OPEN';

-- Covering index для списка турниров
-- Включает все поля для отображения в списке без дополнительных lookup
CREATE INDEX IF NOT EXISTS idx_tournaments_list_view 
    ON tournaments(start_date DESC, status, name, format, max_participants, current_participants)
    WHERE is_public = true;

COMMENT ON INDEX idx_tournaments_public_status IS 'Поиск публичных турниров по статусу';
COMMENT ON INDEX idx_tournaments_organizer_status IS 'Турниры организатора по статусу';
COMMENT ON INDEX idx_tournaments_format_status IS 'Фильтрация по формату и статусу';
COMMENT ON INDEX idx_tournaments_upcoming_public IS 'Предстоящие публичные турниры';
COMMENT ON INDEX idx_tournaments_rating_range IS 'Фильтр по диапазону рейтинга';
COMMENT ON INDEX idx_tournaments_available_slots IS 'Турниры с открытой регистрацией';
COMMENT ON INDEX idx_tournaments_list_view IS 'Covering index для списка турниров';

-- =====================================================
-- User Roles Table - Composite Indexes
-- =====================================================

-- Индекс для поиска пользователей по роли
-- Используется для получения списка админов/организаторов
CREATE INDEX IF NOT EXISTS idx_user_roles_role_user 
    ON user_roles(role, user_id);

COMMENT ON INDEX idx_user_roles_role_user IS 'Поиск пользователей по роли';


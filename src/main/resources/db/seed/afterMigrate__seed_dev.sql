-- =====================================================
-- Flyway afterMigrate callback: Seed Data for Development
-- =====================================================
-- Этот файл загружается ПОСЛЕ всех миграций
-- Только для dev профиля (FLYWAY_PLACEHOLDERS_SEED_DATA=true)
--
-- Использование:
--   1. Локальная разработка: автоматически при spring.profiles.active=dev
--   2. Ручной запуск: flyway -placeholders.seedData=true afterMigrate
--
-- ВАЖНО: Этот файл НЕ выполняется в production!
-- =====================================================

-- Проверка: выполняем только если seedData=true
-- Flyway placeholders: ${seedData}
-- Если placeholder не задан, скрипт пропускается

-- =====================================================
-- 1. Admin User (password: Admin123!)
-- =====================================================
INSERT INTO users (username, email, password, first_name, last_name, enabled)
SELECT 'admin', 'admin@tournament.local', 
       '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.CQPxFxeGMf4c5S',
       'System', 'Administrator', true
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');

-- Admin roles
INSERT INTO user_roles (user_id, role)
SELECT id, 'ADMIN' FROM users WHERE username = 'admin'
    AND NOT EXISTS (
        SELECT 1 FROM user_roles 
        WHERE user_id = (SELECT id FROM users WHERE username = 'admin') 
        AND role = 'ADMIN'
    );

INSERT INTO user_roles (user_id, role)
SELECT id, 'USER' FROM users WHERE username = 'admin'
    AND NOT EXISTS (
        SELECT 1 FROM user_roles 
        WHERE user_id = (SELECT id FROM users WHERE username = 'admin') 
        AND role = 'USER'
    );

-- =====================================================
-- 2. Test User (password: Test1234!)
-- =====================================================
INSERT INTO users (username, email, password, first_name, last_name, enabled)
SELECT 'testuser', 'test@tournament.local', 
       '$2a$12$N4.TvhO7IvxJDL3KEy0uI.4iyZVf8HJQzD9F7UYT8fKuLXVf5x8Iq',
       'Test', 'User', true
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'testuser');

INSERT INTO user_roles (user_id, role)
SELECT id, 'USER' FROM users WHERE username = 'testuser'
    AND NOT EXISTS (
        SELECT 1 FROM user_roles 
        WHERE user_id = (SELECT id FROM users WHERE username = 'testuser') 
        AND role = 'USER'
    );

-- =====================================================
-- 3. Organizer User (password: Organizer123!)
-- =====================================================
INSERT INTO users (username, email, password, first_name, last_name, enabled)
SELECT 'organizer', 'organizer@tournament.local', 
       '$2a$12$rQvJz8Jvx9LFmN8G7vK3qOhC4VpL1zR5mK6nBwX2dT8fS1yA3eC4K',
       'Tournament', 'Organizer', true
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'organizer');

INSERT INTO user_roles (user_id, role)
SELECT id, 'ORGANIZER' FROM users WHERE username = 'organizer'
    AND NOT EXISTS (
        SELECT 1 FROM user_roles 
        WHERE user_id = (SELECT id FROM users WHERE username = 'organizer') 
        AND role = 'ORGANIZER'
    );

INSERT INTO user_roles (user_id, role)
SELECT id, 'USER' FROM users WHERE username = 'organizer'
    AND NOT EXISTS (
        SELECT 1 FROM user_roles 
        WHERE user_id = (SELECT id FROM users WHERE username = 'organizer') 
        AND role = 'USER'
    );

-- =====================================================
-- 4. Sample Tournaments
-- =====================================================
INSERT INTO tournaments (
    name, description, format, status, organizer_id,
    start_date, max_participants, time_control_minutes, time_increment_seconds, is_public
)
SELECT 
    'Spring Championship 2026',
    'Открытый турнир по быстрым шахматам. Приглашаются все желающие!',
    'SWISS', 'DRAFT',
    (SELECT id FROM users WHERE username = 'admin'),
    CURRENT_TIMESTAMP + INTERVAL '7 days',
    32, 15, 10, true
WHERE NOT EXISTS (SELECT 1 FROM tournaments WHERE name = 'Spring Championship 2026');

INSERT INTO tournaments (
    name, description, format, status, organizer_id,
    start_date, end_date, max_participants, time_control_minutes, 
    time_increment_seconds, min_rating, max_rating, prize_pool, entry_fee, is_public
)
SELECT 
    'Summer Blitz Open 2026',
    'Блиц-турнир для игроков среднего уровня',
    'ROUND_ROBIN', 'REGISTRATION_OPEN',
    (SELECT id FROM users WHERE username = 'organizer'),
    CURRENT_TIMESTAMP + INTERVAL '14 days',
    CURRENT_TIMESTAMP + INTERVAL '14 days' + INTERVAL '8 hours',
    16, 5, 3, 1400, 2000, 500, 25, true
WHERE NOT EXISTS (SELECT 1 FROM tournaments WHERE name = 'Summer Blitz Open 2026');

INSERT INTO tournaments (
    name, description, format, status, organizer_id,
    start_date, end_date, max_participants, time_control_minutes, 
    time_increment_seconds, min_rating, prize_pool, is_public
)
SELECT 
    'Masters Invitational 2026',
    'Приглашённый турнир для мастеров. Только по приглашению.',
    'KNOCKOUT', 'DRAFT',
    (SELECT id FROM users WHERE username = 'admin'),
    CURRENT_TIMESTAMP + INTERVAL '30 days',
    CURRENT_TIMESTAMP + INTERVAL '32 days',
    8, 90, 30, 2400, 10000, false
WHERE NOT EXISTS (SELECT 1 FROM tournaments WHERE name = 'Masters Invitational 2026');

-- =====================================================
-- Seed data loaded successfully
-- =====================================================


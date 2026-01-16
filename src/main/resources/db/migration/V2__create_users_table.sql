-- Flyway migration: Create users table
-- Version: 2
-- Description: Creates users table and user_roles for authentication

-- =====================
-- Users Table
-- =====================
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    enabled BOOLEAN NOT NULL DEFAULT true,
    account_non_expired BOOLEAN NOT NULL DEFAULT true,
    account_non_locked BOOLEAN NOT NULL DEFAULT true,
    credentials_non_expired BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP
);

-- Indexes
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_enabled ON users(enabled);

-- Comments
COMMENT ON TABLE users IS 'Пользователи системы турниров';
COMMENT ON COLUMN users.username IS 'Уникальное имя пользователя';
COMMENT ON COLUMN users.email IS 'Email пользователя (уникальный)';
COMMENT ON COLUMN users.password IS 'Хэш пароля (BCrypt)';
COMMENT ON COLUMN users.enabled IS 'Активен ли аккаунт';
COMMENT ON COLUMN users.account_non_locked IS 'Не заблокирован ли аккаунт';

-- =====================
-- User Roles Table
-- =====================
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    PRIMARY KEY (user_id, role)
);

-- Index
CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);

-- Comments
COMMENT ON TABLE user_roles IS 'Роли пользователей (USER, ORGANIZER, ADMIN)';
COMMENT ON COLUMN user_roles.role IS 'Роль: USER, ORGANIZER, ADMIN';

-- =====================================================
-- Триггер для автообновления updated_at
-- =====================================================
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- SEED DATA перенесены в afterMigrate__seed_dev.sql
-- Загружаются только при FLYWAY_PLACEHOLDERS_SEED_DATA=true
-- =====================================================




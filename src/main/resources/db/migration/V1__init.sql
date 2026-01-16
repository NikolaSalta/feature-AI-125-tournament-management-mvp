-- =====================================================
-- Flyway migration: Initial database schema
-- Version: 1
-- Description: Создание расширений и базовых настроек БД
-- =====================================================

-- Включаем расширение для генерации UUID (если понадобится)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Включаем расширение для криптографии (если понадобится)
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =====================================================
-- Функция автообновления updated_at
-- =====================================================
-- Триггерная функция для автоматического обновления поля updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

COMMENT ON FUNCTION update_updated_at_column() IS 'Триггерная функция для автоматического обновления updated_at';

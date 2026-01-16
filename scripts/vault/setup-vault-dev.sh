#!/bin/bash

# =====================================================
# HashiCorp Vault Setup Script (Development)
# =====================================================
# Этот скрипт настраивает Vault для локальной разработки:
# 1. Запускает Vault в dev-режиме (если еще не запущен)
# 2. Включает KV v2 движок
# 3. Записывает тестовые секреты для приложения
#
# Использование:
#   ./setup-vault-dev.sh
#
# Требования:
#   - HashiCorp Vault установлен (https://www.vaultproject.io/downloads)
#   - Порт 8200 свободен

set -e

echo "=========================================="
echo "HashiCorp Vault Setup (Development)"
echo "=========================================="

# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Проверка установки Vault
if ! command -v vault &> /dev/null; then
    echo -e "${RED}❌ Vault не установлен!${NC}"
    echo "Установите Vault: https://www.vaultproject.io/downloads"
    exit 1
fi

echo -e "${GREEN}✓ Vault установлен: $(vault version)${NC}"

# Проверка, запущен ли Vault
if ! curl -s http://127.0.0.1:8200/v1/sys/health > /dev/null 2>&1; then
    echo -e "${YELLOW}⚠️  Vault не запущен. Запускаем в dev-режиме...${NC}"
    echo ""
    echo "Vault будет запущен в отдельном терминале."
    echo "Root Token будет выведен в консоль."
    echo ""
    
    # Запуск Vault в dev-режиме в фоне
    vault server -dev > vault-dev.log 2>&1 &
    VAULT_PID=$!
    
    echo "Vault PID: $VAULT_PID"
    echo "Ждем запуска Vault..."
    sleep 3
    
    # Извлекаем Root Token из лога
    if [ -f vault-dev.log ]; then
        ROOT_TOKEN=$(grep "Root Token:" vault-dev.log | awk '{print $3}')
        echo -e "${GREEN}✓ Vault запущен!${NC}"
        echo -e "${YELLOW}Root Token: $ROOT_TOKEN${NC}"
        echo ""
        echo "Сохраните этот токен! Он понадобится для доступа к Vault."
        echo "Экспортируйте его: export VAULT_TOKEN=$ROOT_TOKEN"
        echo ""
    fi
else
    echo -e "${GREEN}✓ Vault уже запущен${NC}"
fi

# Настройка переменных окружения
export VAULT_ADDR='http://127.0.0.1:8200'

# Проверка VAULT_TOKEN
if [ -z "$VAULT_TOKEN" ]; then
    echo -e "${YELLOW}⚠️  VAULT_TOKEN не установлен${NC}"
    echo "Введите Root Token из вывода 'vault server -dev':"
    read -r VAULT_TOKEN
    export VAULT_TOKEN
fi

echo ""
echo "=========================================="
echo "Настройка секретов"
echo "=========================================="

# Включение KV v2 движка (если еще не включен)
echo "Включаем KV v2 движок..."
vault secrets enable -version=2 -path=secret kv 2>/dev/null || echo "KV движок уже включен"

# Запись секретов для tournament-service-be
echo ""
echo "Записываем секреты для tournament-service-be..."

# Общие секреты (для всех профилей)
vault kv put secret/tournament-service-be \
    db.password="vault_dev_db_password" \
    jwt.secret="vault_super_secret_jwt_key_that_is_at_least_256_bits_long_for_development" \
    jwt.expiration="3600000" \
    jwt.refresh-expiration="604800000"

echo -e "${GREEN}✓ Общие секреты записаны${NC}"

# Секреты для dev профиля
vault kv put secret/tournament-service-be/dev \
    db.password="vault_dev_db_password" \
    db.username="tournament_user" \
    db.host="localhost" \
    db.port="5432" \
    db.name="tournament_db"

echo -e "${GREEN}✓ Секреты для dev профиля записаны${NC}"

# Секреты для prod профиля
vault kv put secret/tournament-service-be/prod \
    db.password="vault_prod_db_password_change_me" \
    db.username="tournament_user_prod" \
    db.host="prod-db-host" \
    db.port="5432" \
    db.name="tournament_db_prod" \
    jwt.secret="vault_prod_super_secret_jwt_key_change_me_in_production"

echo -e "${GREEN}✓ Секреты для prod профиля записаны${NC}"

echo ""
echo "=========================================="
echo "Проверка секретов"
echo "=========================================="

echo ""
echo "Секреты в secret/tournament-service-be:"
vault kv get secret/tournament-service-be

echo ""
echo "Секреты в secret/tournament-service-be/dev:"
vault kv get secret/tournament-service-be/dev

echo ""
echo "=========================================="
echo "✅ Настройка завершена!"
echo "=========================================="
echo ""
echo "Для запуска приложения с Vault:"
echo "  export VAULT_TOKEN=$VAULT_TOKEN"
echo "  export VAULT_ADDR=http://127.0.0.1:8200"
echo "  ./gradlew bootRun --args='--spring.profiles.active=vault,dev'"
echo ""
echo "Для остановки Vault (dev-режим):"
echo "  pkill -f 'vault server -dev'"
echo ""


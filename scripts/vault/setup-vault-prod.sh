#!/bin/bash

# =====================================================
# HashiCorp Vault Setup Script (Production)
# =====================================================
# Этот скрипт настраивает Vault для production-окружения:
# 1. Проверяет подключение к Vault
# 2. Включает KV v2 движок
# 3. Записывает production секреты
# 4. Настраивает политики доступа
#
# Использование:
#   VAULT_ADDR=https://vault.example.com:8200 \
#   VAULT_TOKEN=<admin-token> \
#   ./setup-vault-prod.sh
#
# Требования:
#   - Vault запущен и настроен (unsealed)
#   - VAULT_ADDR и VAULT_TOKEN установлены
#   - Права администратора в Vault

set -e

echo "=========================================="
echo "HashiCorp Vault Setup (Production)"
echo "=========================================="

# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Проверка установки Vault
if ! command -v vault &> /dev/null; then
    echo -e "${RED}❌ Vault не установлен!${NC}"
    exit 1
fi

# Проверка VAULT_ADDR
if [ -z "$VAULT_ADDR" ]; then
    echo -e "${RED}❌ VAULT_ADDR не установлен!${NC}"
    echo "Установите: export VAULT_ADDR=https://vault.example.com:8200"
    exit 1
fi

# Проверка VAULT_TOKEN
if [ -z "$VAULT_TOKEN" ]; then
    echo -e "${RED}❌ VAULT_TOKEN не установлен!${NC}"
    echo "Установите: export VAULT_TOKEN=<admin-token>"
    exit 1
fi

echo -e "${GREEN}✓ Vault URL: $VAULT_ADDR${NC}"

# Проверка подключения к Vault
if ! vault status > /dev/null 2>&1; then
    echo -e "${RED}❌ Не удается подключиться к Vault${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Подключение к Vault установлено${NC}"

echo ""
echo "=========================================="
echo "Настройка KV движка"
echo "=========================================="

# Включение KV v2 движка
vault secrets enable -version=2 -path=secret kv 2>/dev/null || echo "KV движок уже включен"

echo ""
echo "=========================================="
echo "Запись Production секретов"
echo "=========================================="

echo -e "${YELLOW}⚠️  ВНИМАНИЕ: Используйте реальные production секреты!${NC}"
echo ""

# Запрос секретов у пользователя
read -s -p "Введите DB Password для production: " DB_PASSWORD
echo ""
read -s -p "Введите JWT Secret для production (минимум 256 бит): " JWT_SECRET
echo ""

# Запись секретов для production
vault kv put secret/tournament-service-be/prod \
    db.password="$DB_PASSWORD" \
    db.username="${DB_USERNAME:-tournament_user_prod}" \
    db.host="${DB_HOST:-prod-db-host.example.com}" \
    db.port="${DB_PORT:-5432}" \
    db.name="${DB_NAME:-tournament_db_prod}" \
    jwt.secret="$JWT_SECRET" \
    jwt.expiration="3600000" \
    jwt.refresh-expiration="604800000"

echo -e "${GREEN}✓ Production секреты записаны${NC}"

echo ""
echo "=========================================="
echo "Настройка политики доступа"
echo "=========================================="

# Создание политики для tournament-service-be
cat > tournament-service-policy.hcl <<EOF
# Политика для Tournament Service Backend
path "secret/data/tournament-service-be" {
  capabilities = ["read"]
}

path "secret/data/tournament-service-be/prod" {
  capabilities = ["read"]
}
EOF

vault policy write tournament-service-be tournament-service-policy.hcl
rm tournament-service-policy.hcl

echo -e "${GREEN}✓ Политика создана${NC}"

echo ""
echo "=========================================="
echo "Создание токена для приложения"
echo "=========================================="

# Создание токена с ограниченными правами
APP_TOKEN=$(vault token create \
    -policy=tournament-service-be \
    -ttl=720h \
    -renewable=true \
    -format=json | jq -r '.auth.client_token')

echo -e "${GREEN}✓ Токен для приложения создан${NC}"
echo ""
echo -e "${YELLOW}Application Token:${NC}"
echo "$APP_TOKEN"
echo ""
echo "Сохраните этот токен в безопасном месте!"
echo "Используйте его в переменной окружения VAULT_TOKEN для приложения."

echo ""
echo "=========================================="
echo "✅ Production настройка завершена!"
echo "=========================================="
echo ""
echo "Для запуска приложения:"
echo "  export VAULT_TOKEN=$APP_TOKEN"
echo "  export VAULT_ADDR=$VAULT_ADDR"
echo "  export VAULT_SCHEME=https"
echo "  ./gradlew bootRun --args='--spring.profiles.active=vault,prod'"
echo ""


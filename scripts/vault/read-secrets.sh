#!/bin/bash

# =====================================================
# Read Secrets from Vault
# =====================================================
# Скрипт для чтения секретов из Vault
#
# Использование:
#   ./read-secrets.sh [profile]
#
# Примеры:
#   ./read-secrets.sh         # Читает общие секреты
#   ./read-secrets.sh dev     # Читает секреты для dev профиля
#   ./read-secrets.sh prod    # Читает секреты для prod профиля

set -e

PROFILE=${1:-""}
APP_NAME="tournament-service-be"

# Цвета
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Настройка Vault
export VAULT_ADDR=${VAULT_ADDR:-'http://127.0.0.1:8200'}

if [ -z "$VAULT_TOKEN" ]; then
    echo -e "${YELLOW}⚠️  VAULT_TOKEN не установлен${NC}"
    echo "Установите: export VAULT_TOKEN=<your-token>"
    exit 1
fi

echo "=========================================="
echo "Reading secrets from Vault"
echo "=========================================="
echo "Vault: $VAULT_ADDR"
echo "App: $APP_NAME"
echo "Profile: ${PROFILE:-default}"
echo "=========================================="
echo ""

if [ -z "$PROFILE" ]; then
    # Читаем общие секреты
    echo -e "${GREEN}Общие секреты:${NC}"
    vault kv get secret/$APP_NAME
else
    # Читаем секреты для профиля
    echo -e "${GREEN}Секреты для профиля '$PROFILE':${NC}"
    vault kv get secret/$APP_NAME/$PROFILE
fi

echo ""




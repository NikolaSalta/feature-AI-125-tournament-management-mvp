# 🔐 HashiCorp Vault - Краткая инструкция

## Быстрый старт для разработки

### 1. Установка Vault (если еще не установлен)

**macOS:**
```bash
brew install vault
```

**Linux:**
```bash
wget -O- https://apt.releases.hashicorp.com/gpg | sudo gpg --dearmor -o /usr/share/keyrings/hashicorp-archive-keyring.gpg
echo "deb [signed-by=/usr/share/keyrings/hashicorp-archive-keyring.gpg] https://apt.releases.hashicorp.com $(lsb_release -cs) main" | sudo tee /etc/apt/sources.list.d/hashicorp.list
sudo apt update && sudo apt install vault
```

**Проверка:**
```bash
vault version
```

### 2. Запуск Vault в dev-режиме

```bash
cd scripts/vault
./setup-vault-dev.sh
```

Скрипт автоматически:
- Запустит Vault в dev-режиме
- Создаст KV v2 движок
- Запишет тестовые секреты
- Выведет Root Token

**Сохраните Root Token!** Он понадобится для доступа к Vault.

### 3. Экспорт переменных окружения

```bash
# Токен из вывода скрипта
export VAULT_TOKEN="hvs.CAESIJ..."
export VAULT_ADDR="http://127.0.0.1:8200"
```

### 4. Проверка секретов

```bash
# Проверить общие секреты
./scripts/vault/read-secrets.sh

# Проверить dev секреты
./scripts/vault/read-secrets.sh dev

# Проверить prod секреты
./scripts/vault/read-secrets.sh prod
```

### 5. Запуск приложения с Vault

```bash
# Вариант 1: Через Gradle
./gradlew bootRun --args='--spring.profiles.active=vault,dev'

# Вариант 2: Через переменную окружения
export SPRING_PROFILES_ACTIVE=vault,dev
./gradlew bootRun

# Вариант 3: JAR файл
java -jar build/libs/tournament-service-be-*.jar --spring.profiles.active=vault,dev
```

### 6. Проверка подключения к Vault

При успешном подключении в логах вы увидите:

```
================================================================================
HashiCorp Vault Configuration
================================================================================
Vault URL: http://127.0.0.1:8200
Authentication Method: TOKEN
KV Backend: secret
Default Context (Application): tournament-service-be
Vault Profile: ACTIVE
================================================================================
```

---

## Структура секретов в Vault

```
secret/
├── tournament-service-be/           # Общие секреты для всех профилей
│   ├── jwt.secret
│   ├── jwt.expiration
│   └── jwt.refresh-expiration
│
├── tournament-service-be/dev/       # Секреты для dev профиля
│   ├── db.host
│   ├── db.port
│   ├── db.name
│   ├── db.username
│   └── db.password
│
└── tournament-service-be/prod/      # Секреты для prod профиля
    ├── db.host
    ├── db.port
    ├── db.name
    ├── db.username
    ├── db.password
    └── jwt.secret
```

---

## Управление секретами

### Чтение секретов

```bash
# Общие секреты
vault kv get secret/tournament-service-be

# Dev секреты
vault kv get secret/tournament-service-be/dev

# Конкретный ключ
vault kv get -field=db.password secret/tournament-service-be/dev
```

### Запись секретов

```bash
# Обновить один секрет
vault kv put secret/tournament-service-be/dev db.password="new_password"

# Обновить несколько секретов
vault kv put secret/tournament-service-be/dev \
    db.host="localhost" \
    db.port="5432" \
    db.password="new_password"
```

### Удаление секретов

```bash
# Удалить последнюю версию
vault kv delete secret/tournament-service-be/dev

# Удалить конкретную версию
vault kv delete -versions=2 secret/tournament-service-be/dev

# Восстановить удаленную версию
vault kv undelete -versions=2 secret/tournament-service-be/dev
```

---

## Профили Spring

| Профиль | Описание | Vault |
|---------|----------|-------|
| `dev` | Разработка без Vault | ❌ |
| `prod` | Production без Vault | ❌ |
| `vault,dev` | Разработка с Vault | ✅ |
| `vault,prod` | Production с Vault | ✅ |

---

## Остановка Vault

```bash
# Найти процесс Vault
ps aux | grep vault

# Остановить (dev-режим)
pkill -f 'vault server -dev'

# Или по PID
kill <PID>
```

---

## Troubleshooting

### Проблема: "Connection refused"

**Решение:**
```bash
# Проверить, запущен ли Vault
curl http://127.0.0.1:8200/v1/sys/health

# Запустить Vault
cd scripts/vault && ./setup-vault-dev.sh
```

### Проблема: "Permission denied"

**Решение:**
```bash
# Проверить токен
vault token lookup

# Установить правильный токен
export VAULT_TOKEN="hvs.CAESIJ..."
```

### Проблема: Приложение не находит секреты

**Решение:**
```bash
# Проверить структуру секретов
vault kv list secret/
vault kv get secret/tournament-service-be
vault kv get secret/tournament-service-be/dev

# Убедиться, что профиль vault активен
export SPRING_PROFILES_ACTIVE=vault,dev
```

---

## Docker Compose с Vault

```bash
# Запустить все сервисы включая Vault
docker-compose --profile vault up -d

# Настроить секреты в Vault контейнере
docker exec -it tournament-vault sh
export VAULT_ADDR='http://127.0.0.1:8200'
export VAULT_TOKEN='dev-root-token'

# Записать секреты
vault kv put secret/tournament-service-be/prod \
    db.host="postgres" \
    db.port="5432" \
    db.name="tournament_db" \
    db.username="tournament_user" \
    db.password="your_password"
```

---

## Полная документация

Для детальной информации см. [docs/VAULT_SETUP.md](docs/VAULT_SETUP.md)

---

## Важные файлы

| Файл | Назначение |
|------|------------|
| `bootstrap.yml` | Конфигурация Vault (загружается первым) |
| `application-vault.yml` | Переопределения для профиля vault |
| `VaultConfig.java` | Java конфигурация Vault |
| `scripts/vault/setup-vault-dev.sh` | Автоматическая настройка для dev |
| `scripts/vault/setup-vault-prod.sh` | Настройка для production |
| `scripts/vault/read-secrets.sh` | Чтение секретов |

---

## Безопасность

⚠️ **ВАЖНО:**
- Не коммитьте токены в Git
- Используйте HTTPS в production
- Не используйте Root Token для приложений
- Создавайте отдельные токены с минимальными правами
- Включите аудит в production

✅ **Рекомендации:**
- Используйте Kubernetes Auth в K8s
- Настройте Auto-Unseal для production
- Регулярно ротируйте токены
- Мониторьте доступ к секретам


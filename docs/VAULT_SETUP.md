# HashiCorp Vault Integration

Полное руководство по интеграции HashiCorp Vault в Tournament Service Backend для централизованного управления секретами.

## Содержание

1. [Обзор](#обзор)
2. [Что было добавлено](#что-было-добавлено)
3. [Установка Vault](#установка-vault)
4. [Быстрый старт (Development)](#быстрый-старт-development)
5. [Production Setup](#production-setup)
6. [Конфигурация](#конфигурация)
7. [Структура секретов в Vault](#структура-секретов-в-vault)
8. [Использование секретов](#использование-секретов)
9. [Методы аутентификации](#методы-аутентификации)
10. [Troubleshooting](#troubleshooting)

---

## Обзор

HashiCorp Vault — это инструмент для безопасного хранения и управления секретами (пароли, API ключи, токены и т.д.). В отличие от хранения секретов в переменных окружения или зашифрованных файлах (Git-crypt), Vault предоставляет:

- **Централизованное хранилище** секретов
- **Динамическую выдачу** секретов по запросу
- **Аудит** всех обращений к секретам
- **Ротацию** секретов без перезапуска приложения
- **Детальный контроль доступа** через политики
- **Шифрование** данных в покое и в движении

### Преимущества для нашего проекта

| Функция | Git-crypt | Переменные окружения | HashiCorp Vault |
|---------|-----------|----------------------|-----------------|
| Централизованное управление | ❌ | ❌ | ✅ |
| Динамическая выдача | ❌ | ❌ | ✅ |
| Аудит доступа | ❌ | ❌ | ✅ |
| Ротация без перезапуска | ❌ | ❌ | ✅ |
| Версионирование секретов | ❌ | ❌ | ✅ |
| Контроль доступа (ACL) | ❌ | ❌ | ✅ |

---

## Что было добавлено

### 1. Зависимости в `pom.xml`

```xml
<!-- Spring Cloud Dependencies Management -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>2023.0.3</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- HashiCorp Vault -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-vault-config</artifactId>
</dependency>
```

**Назначение:**
- `spring-cloud-dependencies` — управляет версиями всех Spring Cloud компонентов
- `spring-cloud-starter-vault-config` — интеграция с Vault для загрузки конфигурации

### 2. Конфигурационные файлы

#### `application-vault.yml`

Новый профиль для активации Vault:

```yaml
spring:
  config:
    import: vault:// # Импорт конфигурации из Vault

  cloud:
    vault:
      host: ${VAULT_HOST:127.0.0.1}
      port: ${VAULT_PORT:8200}
      scheme: ${VAULT_SCHEME:http}
      authentication: TOKEN
      token: ${VAULT_TOKEN}
      
      kv:
        enabled: true
        backend: secret
        default-context: ${spring.application.name}
        application-name: ${spring.application.name}
        profile-separator: /
      
      fail-fast: true
```

**Параметры:**
- `host/port/scheme` — адрес Vault сервера
- `authentication` — метод аутентификации (TOKEN, KUBERNETES, AWS_IAM и др.)
- `token` — токен для доступа к Vault
- `kv.backend` — путь к KV движку (обычно `secret`)
- `kv.default-context` — имя приложения для формирования пути к секретам
- `fail-fast` — остановить запуск при ошибке подключения к Vault

#### Обновленный `application.yml`

```yaml
spring:
  config:
    import: optional:vault:// # optional = не падать, если Vault недоступен

  datasource:
    # Приоритет: Vault > Переменные окружения > Значения по умолчанию
    url: jdbc:postgresql://${db.host:${DB_HOST:localhost}}:${db.port:${DB_PORT:5432}}/${db.name:${DB_NAME:tournament_db}}
    username: ${db.username:${DB_USERNAME:tournament_user}}
    password: ${db.password:${DB_PASSWORD}}

jwt:
  secret: ${jwt.secret:${JWT_SECRET:default_value}}
  access-token-expiration: ${jwt.expiration:${JWT_ACCESS_EXPIRATION:900000}}
```

**Логика загрузки:**
1. Сначала проверяется Vault (если профиль `vault` активен)
2. Затем переменные окружения
3. Затем значения по умолчанию

### 3. Java конфигурация

#### `VaultConfig.java`

```java
@Configuration
@Profile("vault")
@ConditionalOnProperty(name = "spring.cloud.vault.enabled", havingValue = "true", matchIfMissing = true)
public class VaultConfig {
    
    @PostConstruct
    public void init() {
        // Логирует информацию о подключении к Vault
        logger.info("Vault URL: {}://{}:{}", vaultScheme, vaultHost, vaultPort);
        logger.info("Authentication Method: {}", authMethod);
    }
    
    public String getSecretPath() {
        return String.format("%s/data/%s", kvBackend, defaultContext);
    }
}
```

**Назначение:**
- Активируется только при профиле `vault`
- Проверяет конфигурацию Vault при старте
- Предоставляет утилитные методы для работы с путями

### 4. Скрипты настройки

#### `scripts/vault/setup-vault-dev.sh`

Автоматическая настройка Vault для разработки:

```bash
#!/bin/bash
# 1. Запускает Vault в dev-режиме (если не запущен)
# 2. Включает KV v2 движок
# 3. Записывает тестовые секреты
```

**Что делает:**
- Проверяет установку Vault
- Запускает Vault в dev-режиме
- Создает структуру секретов:
  - `secret/tournament-service-be` — общие секреты
  - `secret/tournament-service-be/dev` — секреты для dev
  - `secret/tournament-service-be/prod` — секреты для prod

#### `scripts/vault/setup-vault-prod.sh`

Настройка Vault для production:

```bash
#!/bin/bash
# 1. Проверяет подключение к Vault
# 2. Создает политики доступа
# 3. Генерирует токен для приложения
```

**Что делает:**
- Запрашивает production секреты у администратора
- Создает политику с минимальными правами
- Генерирует токен для приложения (TTL: 720h, renewable)

#### `scripts/vault/read-secrets.sh`

Утилита для чтения секретов:

```bash
./read-secrets.sh         # Общие секреты
./read-secrets.sh dev     # Dev секреты
./read-secrets.sh prod    # Prod секреты
```

---

## Установка Vault

### macOS

```bash
brew tap hashicorp/tap
brew install hashicorp/tap/vault
```

### Linux

```bash
wget -O- https://apt.releases.hashicorp.com/gpg | sudo gpg --dearmor -o /usr/share/keyrings/hashicorp-archive-keyring.gpg
echo "deb [signed-by=/usr/share/keyrings/hashicorp-archive-keyring.gpg] https://apt.releases.hashicorp.com $(lsb_release -cs) main" | sudo tee /etc/apt/sources.list.d/hashicorp.list
sudo apt update && sudo apt install vault
```

### Windows

Скачайте с [официального сайта](https://www.vaultproject.io/downloads) и добавьте в PATH.

### Проверка установки

```bash
vault version
# Vault v1.15.0 (или выше)
```

---

## Быстрый старт (Development)

### Шаг 1: Запуск и настройка Vault

```bash
cd scripts/vault
./setup-vault-dev.sh
```

Скрипт выведет Root Token:

```
Root Token: hvs.CAESIJ...
```

**Сохраните этот токен!**

### Шаг 2: Экспорт переменных окружения

```bash
export VAULT_TOKEN="hvs.CAESIJ..."  # Ваш Root Token
export VAULT_ADDR="http://127.0.0.1:8200"
```

### Шаг 3: Проверка секретов

```bash
./read-secrets.sh dev
```

Вывод:

```
====== Secret Path ======
secret/data/tournament-service-be/dev

======= Metadata =======
Key                Value
---                -----
created_time       2024-01-02T10:00:00Z
version            1

====== Data ======
Key           Value
---           -----
db.host       localhost
db.name       tournament_db
db.password   vault_dev_db_password
db.port       5432
db.username   tournament_user
```

### Шаг 4: Запуск приложения с Vault

```bash
# Вариант 1: Maven
./mvnw spring-boot:run -Dspring-boot.run.profiles=vault,dev

# Вариант 2: Переменные окружения
export SPRING_PROFILES_ACTIVE=vault,dev
./mvnw spring-boot:run

# Вариант 3: JAR
java -jar target/tournament-service-be-1.0.0-MVP.jar --spring.profiles.active=vault,dev
```

### Шаг 5: Проверка логов

При успешном подключении к Vault вы увидите:

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

## Production Setup

### Шаг 1: Запуск Vault в production режиме

**Не используйте dev-режим в production!**

Пример конфигурации Vault (`vault.hcl`):

```hcl
storage "consul" {
  address = "127.0.0.1:8500"
  path    = "vault/"
}

listener "tcp" {
  address     = "0.0.0.0:8200"
  tls_cert_file = "/etc/vault/tls/vault.crt"
  tls_key_file  = "/etc/vault/tls/vault.key"
}

api_addr = "https://vault.example.com:8200"
cluster_addr = "https://vault.example.com:8201"
ui = true
```

Запуск:

```bash
vault server -config=vault.hcl
```

### Шаг 2: Инициализация и Unseal

```bash
# Инициализация (только один раз!)
vault operator init

# Сохраните Unseal Keys и Root Token!

# Unseal (требуется 3 из 5 ключей)
vault operator unseal <unseal-key-1>
vault operator unseal <unseal-key-2>
vault operator unseal <unseal-key-3>
```

### Шаг 3: Настройка секретов и политик

```bash
export VAULT_ADDR="https://vault.example.com:8200"
export VAULT_TOKEN="<admin-token>"

cd scripts/vault
./setup-vault-prod.sh
```

Скрипт запросит production секреты и создаст токен для приложения.

### Шаг 4: Запуск приложения

```bash
export VAULT_TOKEN="<app-token>"  # Из вывода setup-vault-prod.sh
export VAULT_ADDR="https://vault.example.com:8200"
export VAULT_SCHEME="https"
export SPRING_PROFILES_ACTIVE="vault,prod"

java -jar tournament-service-be.jar
```

### Рекомендации для Production

1. **Используйте HTTPS** для Vault
2. **Не используйте Root Token** для приложений
3. **Настройте Auto-Unseal** (AWS KMS, Azure Key Vault, GCP KMS)
4. **Включите аудит**:
   ```bash
   vault audit enable file file_path=/var/log/vault_audit.log
   ```
5. **Настройте мониторинг** Vault через Prometheus/Grafana
6. **Используйте Kubernetes Auth** если деплоите в K8s
7. **Настройте ротацию токенов**

---

## Конфигурация

### Переменные окружения

| Переменная | Описание | Значение по умолчанию |
|------------|----------|----------------------|
| `VAULT_TOKEN` | Токен для доступа к Vault | - (обязательно) |
| `VAULT_ADDR` | URL Vault сервера | `http://127.0.0.1:8200` |
| `VAULT_HOST` | Хост Vault | `127.0.0.1` |
| `VAULT_PORT` | Порт Vault | `8200` |
| `VAULT_SCHEME` | Протокол (http/https) | `http` |
| `SPRING_PROFILES_ACTIVE` | Активные профили | `dev` |

### Профили Spring

| Профиль | Описание | Использование |
|---------|----------|---------------|
| `dev` | Разработка | Локальная разработка |
| `prod` | Production | Production окружение |
| `vault` | Vault интеграция | Добавить к dev или prod |

**Примеры комбинаций:**

```bash
# Dev без Vault (переменные окружения)
SPRING_PROFILES_ACTIVE=dev

# Dev с Vault
SPRING_PROFILES_ACTIVE=vault,dev

# Prod с Vault
SPRING_PROFILES_ACTIVE=vault,prod
```

---

## Структура секретов в Vault

Spring Cloud Vault ищет секреты в следующих путях (в порядке приоритета):

```
secret/tournament-service-be/<profile>  # Специфичные для профиля
secret/tournament-service-be            # Общие для всех профилей
```

### Пример структуры

```
secret/
├── tournament-service-be/           # Общие секреты
│   ├── jwt.secret
│   ├── jwt.expiration
│   └── jwt.refresh-expiration
│
├── tournament-service-be/dev/       # Dev секреты
│   ├── db.host
│   ├── db.port
│   ├── db.name
│   ├── db.username
│   └── db.password
│
└── tournament-service-be/prod/      # Prod секреты
    ├── db.host
    ├── db.port
    ├── db.name
    ├── db.username
    └── db.password
```

### Запись секретов вручную

```bash
# Общие секреты
vault kv put secret/tournament-service-be \
    jwt.secret="my_super_secret_key" \
    jwt.expiration="3600000"

# Dev секреты
vault kv put secret/tournament-service-be/dev \
    db.host="localhost" \
    db.port="5432" \
    db.name="tournament_db" \
    db.username="tournament_user" \
    db.password="dev_password"

# Prod секреты
vault kv put secret/tournament-service-be/prod \
    db.host="prod-db.example.com" \
    db.port="5432" \
    db.name="tournament_db_prod" \
    db.username="tournament_user_prod" \
    db.password="prod_secure_password"
```

### Чтение секретов

```bash
# Общие
vault kv get secret/tournament-service-be

# Dev
vault kv get secret/tournament-service-be/dev

# Prod
vault kv get secret/tournament-service-be/prod

# Конкретный ключ
vault kv get -field=db.password secret/tournament-service-be/dev
```

### Версионирование секретов

KV v2 автоматически версионирует секреты:

```bash
# Получить версию 2
vault kv get -version=2 secret/tournament-service-be/dev

# Посмотреть метаданные
vault kv metadata get secret/tournament-service-be/dev

# Откатить к версии 1
vault kv rollback -version=1 secret/tournament-service-be/dev

# Удалить версию 3
vault kv delete -versions=3 secret/tournament-service-be/dev

# Восстановить версию 3
vault kv undelete -versions=3 secret/tournament-service-be/dev
```

---

## Использование секретов

### В Java коде

```java
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MyService {

    // Секрет из Vault
    @Value("${db.password}")
    private String dbPassword;

    @Value("${jwt.secret}")
    private String jwtSecret;

    public void useSecrets() {
        // Секреты автоматически загружены из Vault
        System.out.println("DB Password: " + dbPassword);
    }
}
```

### В конфигурационных классах

```java
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "db")
public class DatabaseProperties {
    private String host;
    private int port;
    private String name;
    private String username;
    private String password; // Из Vault
}
```

### Приоритет загрузки

Spring загружает свойства в следующем порядке (последнее перезаписывает предыдущее):

1. `application.yml` (значения по умолчанию)
2. `application-{profile}.yml`
3. Vault: `secret/tournament-service-be`
4. Vault: `secret/tournament-service-be/{profile}`
5. Переменные окружения
6. Аргументы командной строки

**Пример:**

```yaml
# application.yml
db:
  password: default_password

# Vault: secret/tournament-service-be/dev
db.password: vault_password

# Переменная окружения
DB_PASSWORD=env_password
```

Итоговое значение: `env_password` (переменные окружения имеют наивысший приоритет)

---

## Методы аутентификации

### TOKEN (для dev и простых случаев)

```yaml
spring:
  cloud:
    vault:
      authentication: TOKEN
      token: ${VAULT_TOKEN}
```

**Использование:**
```bash
export VAULT_TOKEN="hvs.CAESIJ..."
```

### KUBERNETES (рекомендуется для K8s)

```yaml
spring:
  cloud:
    vault:
      authentication: KUBERNETES
      kubernetes:
        role: tournament-service
        kubernetes-path: kubernetes
        service-account-token-file: /var/run/secrets/kubernetes.io/serviceaccount/token
```

**Настройка в Vault:**
```bash
# Включить Kubernetes auth
vault auth enable kubernetes

# Настроить
vault write auth/kubernetes/config \
    kubernetes_host="https://kubernetes.default.svc:443" \
    kubernetes_ca_cert=@/var/run/secrets/kubernetes.io/serviceaccount/ca.crt \
    token_reviewer_jwt=@/var/run/secrets/kubernetes.io/serviceaccount/token

# Создать роль
vault write auth/kubernetes/role/tournament-service \
    bound_service_account_names=tournament-service \
    bound_service_account_namespaces=default \
    policies=tournament-service-be \
    ttl=24h
```

### AWS IAM (для AWS)

```yaml
spring:
  cloud:
    vault:
      authentication: AWS_IAM
      aws-iam:
        role: tournament-service-role
        aws-path: aws
        server-id: vault.example.com
```

### APPROLE (универсальный для CI/CD)

```yaml
spring:
  cloud:
    vault:
      authentication: APPROLE
      app-role:
        role-id: ${VAULT_ROLE_ID}
        secret-id: ${VAULT_SECRET_ID}
        role: tournament-service
        app-role-path: approle
```

**Настройка:**
```bash
# Включить AppRole
vault auth enable approle

# Создать роль
vault write auth/approle/role/tournament-service \
    secret_id_ttl=24h \
    token_ttl=1h \
    token_max_ttl=4h \
    policies=tournament-service-be

# Получить Role ID
vault read auth/approle/role/tournament-service/role-id

# Сгенерировать Secret ID
vault write -f auth/approle/role/tournament-service/secret-id
```

---

## Troubleshooting

### Проблема: "Connection refused" при подключении к Vault

**Причина:** Vault не запущен или недоступен.

**Решение:**
```bash
# Проверить, запущен ли Vault
curl http://127.0.0.1:8200/v1/sys/health

# Запустить Vault в dev-режиме
vault server -dev

# Или проверить статус production Vault
vault status
```

### Проблема: "Permission denied" при чтении секретов

**Причина:** Токен не имеет прав на чтение секретов.

**Решение:**
```bash
# Проверить политики токена
vault token lookup

# Создать политику с правами на чтение
cat > tournament-policy.hcl <<EOF
path "secret/data/tournament-service-be/*" {
  capabilities = ["read"]
}
EOF

vault policy write tournament-service-be tournament-policy.hcl

# Создать токен с этой политикой
vault token create -policy=tournament-service-be
```

### Проблема: "Vault is sealed"

**Причина:** Vault в запечатанном состоянии (после перезапуска).

**Решение:**
```bash
# Проверить статус
vault status

# Распечатать (unseal) - требуется 3 из 5 ключей
vault operator unseal <key-1>
vault operator unseal <key-2>
vault operator unseal <key-3>
```

### Проблема: Приложение не находит секреты

**Причина:** Неправильный путь или имя приложения.

**Решение:**
```bash
# Проверить имя приложения в application.yml
spring.application.name: tournament-service-be

# Проверить путь к секретам
vault kv list secret/

# Убедиться, что секреты существуют
vault kv get secret/tournament-service-be
vault kv get secret/tournament-service-be/dev
```

### Проблема: "Token expired"

**Причина:** Токен истек (TTL закончился).

**Решение:**
```bash
# Обновить токен (если renewable)
vault token renew

# Или создать новый токен
vault token create -policy=tournament-service-be -ttl=720h -renewable=true
```

### Включение debug логов

Для диагностики проблем с Vault:

```yaml
logging:
  level:
    org.springframework.cloud.vault: DEBUG
    org.springframework.vault: DEBUG
    com.chessai.tournament.config.VaultConfig: DEBUG
```

---

## Best Practices

### 1. Безопасность

- ✅ **Используйте HTTPS** в production
- ✅ **Не используйте Root Token** для приложений
- ✅ **Создавайте отдельные токены** с минимальными правами
- ✅ **Включите аудит** для отслеживания доступа
- ✅ **Ротируйте токены** регулярно
- ❌ **Не храните токены** в Git
- ❌ **Не используйте dev-режим** в production

### 2. Организация секретов

- ✅ **Используйте профили** для разделения окружений
- ✅ **Версионируйте секреты** (KV v2)
- ✅ **Документируйте** структуру секретов
- ✅ **Используйте понятные имена** ключей

### 3. Мониторинг

- ✅ **Мониторьте статус** Vault
- ✅ **Настройте алерты** на unseal events
- ✅ **Отслеживайте истечение** токенов
- ✅ **Анализируйте аудит логи**

### 4. Backup

- ✅ **Делайте backup** Vault storage
- ✅ **Храните Unseal Keys** в безопасном месте
- ✅ **Тестируйте восстановление** регулярно

---

## Дополнительные ресурсы

- [HashiCorp Vault Documentation](https://www.vaultproject.io/docs)
- [Spring Cloud Vault Reference](https://docs.spring.io/spring-cloud-vault/docs/current/reference/html/)
- [Vault Best Practices](https://learn.hashicorp.com/tutorials/vault/production-hardening)
- [Vault Security Model](https://www.vaultproject.io/docs/internals/security)

---

## Changelog

### v0.4.0 - HashiCorp Vault Integration

**Дата:** 2026-01-02

**Добавлено:**
- Spring Cloud Vault зависимости
- Профиль `vault` для конфигурации
- `VaultConfig.java` для управления подключением
- Скрипты автоматической настройки:
  - `setup-vault-dev.sh` — для разработки
  - `setup-vault-prod.sh` — для production
  - `read-secrets.sh` — для чтения секретов
- Поддержка загрузки секретов из Vault в `application.yml`
- Документация `VAULT_SETUP.md`

**Изменено:**
- `application.yml` — добавлен импорт из Vault
- Приоритет загрузки свойств: Vault → Env → Defaults

**Безопасность:**
- Централизованное управление секретами
- Динамическая выдача токенов
- Аудит доступа к секретам
- Версионирование секретов




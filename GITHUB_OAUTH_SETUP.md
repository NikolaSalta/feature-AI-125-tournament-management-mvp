# 🔐 GitHub OAuth2 Setup & Testing Guide

## ✅ Что реализовано

GitHub OAuth2 аутентификация для Tournament Service Backend в development/MVP режиме:

- ✅ **OAuth2 Client** - Spring Boot интеграция с GitHub
- ✅ **Dual Authentication** - OAuth2 + JWT (для API)
- ✅ **MeController** - тестирование OAuth2 flow
- ✅ **Security Configuration** - раздельные filter chains
- ✅ **Development Ready** - готово к тестированию

---

## 🚀 Быстрый старт

### 1. Регистрация GitHub OAuth App

1. **Перейдите в GitHub Settings:**
   ```
   👉 https://github.com/settings/developers
   ```

2. **Создайте новое приложение:**
   - Нажмите **"New OAuth App"**
   - Заполните форму:

   | Поле | Значение |
   |------|----------|
   | **Application name** | `Tournament Service (Dev)` |
   | **Homepage URL** | `http://localhost:8080` |
   | **Authorization callback URL** | `http://localhost:8080/login/oauth2/code/github` |

3. **Сохраните credentials:**
   - Скопируйте **Client ID**
   - Сгенерируйте и скопируйте **Client Secret**

### 2. Настройка Environment Variables

```bash
# Создайте .env файл (если нет)
cp .env.example .env

# Добавьте GitHub credentials в .env
echo "GITHUB_CLIENT_ID=your_actual_client_id" >> .env
echo "GITHUB_CLIENT_SECRET=your_actual_client_secret" >> .env
```

### 3. Запуск приложения

```bash
# Загрузите environment variables
export $(cat .env | xargs)

# Запустите приложение
./gradlew bootRun

# Или через JAR
./gradlew build
java -jar build/libs/tournament-service-be-1.0.0-MVP.jar
```

---

## 🧪 Тестирование OAuth2 Flow

### Сценарий 1: Полный OAuth2 Flow

1. **Откройте браузер:**
   ```
   http://localhost:8080/api/me
   ```

2. **Ожидаемое поведение:**
   - ✅ Автоматический редирект на GitHub
   - ✅ Форма авторизации GitHub
   - ✅ После логина - возврат на `/api/me`
   - ✅ JSON ответ с профилем пользователя

3. **Пример успешного ответа:**
   ```json
   {
     "user": {
       "id": 12345678,
       "login": "your-username",
       "name": "Your Name",
       "email": "your@email.com",
       "avatar_url": "https://avatars.githubusercontent.com/u/12345678",
       "html_url": "https://github.com/your-username"
     },
     "authenticated": true,
     "provider": "github",
     "authorities": ["ROLE_USER", "OAUTH2_USER"],
     "profile": {
       "id": 12345678,
       "username": "your-username",
       "name": "Your Name",
       "email": "your@email.com",
       "avatarUrl": "https://avatars.githubusercontent.com/u/12345678",
       "profileUrl": "https://github.com/your-username"
     }
   }
   ```

### Сценарий 2: Прямой OAuth2 Login

```bash
# Прямая ссылка на GitHub авторизацию
curl -v http://localhost:8080/oauth2/authorization/github
```

### Сценарий 3: User Authorities

```bash
# После успешной авторизации
curl -b cookies.txt http://localhost:8080/api/me/authorities
```

---

## 🏗 Архитектура

### Dual Security Configuration

```
┌─────────────────────────────────────────────────────┐
│                 HTTP Request                        │
└─────────────────────┬───────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────┐
│            Spring Security Filter Chain             │
├─────────────────────────────────────────────────────┤
│  @Order(1) OAuth2SecurityConfig                    │
│  • Matches: /oauth2/**, /login/**, /api/me         │
│  • Handles: GitHub OAuth2 flow                     │
├─────────────────────────────────────────────────────┤
│  @Order(2) ProductionSecurityConfig                │
│  • Matches: /api/**, /actuator/**                  │
│  • Handles: JWT authentication                     │
└─────────────────────────────────────────────────────┘
```

### OAuth2 Flow

```
User Browser          Spring Boot           GitHub
     │                     │                   │
     │ GET /api/me         │                   │
     ├────────────────────▶│                   │
     │                     │                   │
     │ 302 /oauth2/auth..  │                   │
     │◀────────────────────┤                   │
     │                     │                   │
     │ GET /oauth2/authorization/github        │
     ├────────────────────▶│                   │
     │                     │                   │
     │ 302 github.com/login/oauth/authorize    │
     │◀────────────────────┤                   │
     │                     │                   │
     │ GitHub Login Form   │                   │
     ├─────────────────────────────────────────▶│
     │                     │                   │
     │ 302 /login/oauth2/code/github?code=...  │
     │◀─────────────────────────────────────────┤
     │                     │                   │
     │ GET /login/oauth2/code/github?code=...  │
     ├────────────────────▶│                   │
     │                     │ Exchange code     │
     │                     ├──────────────────▶│
     │                     │ Access token      │
     │                     │◀──────────────────┤
     │                     │ Get user info     │
     │                     ├──────────────────▶│
     │                     │ User profile      │
     │                     │◀──────────────────┤
     │ 302 /api/me         │                   │
     │◀────────────────────┤                   │
     │                     │                   │
     │ GET /api/me         │                   │
     ├────────────────────▶│                   │
     │ JSON Profile        │                   │
     │◀────────────────────┤                   │
```

---

## 🔧 Troubleshooting

### Проблема: "Invalid client_id"

**Причина:** Неправильный GITHUB_CLIENT_ID

**Решение:**
```bash
# Проверьте переменные окружения
echo $GITHUB_CLIENT_ID
echo $GITHUB_CLIENT_SECRET

# Проверьте .env файл
cat .env | grep GITHUB
```

### Проблема: "Redirect URI mismatch"

**Причина:** Неправильный callback URL в GitHub OAuth App

**Решение:**
1. Перейдите в GitHub OAuth App settings
2. Убедитесь что **Authorization callback URL** = `http://localhost:8080/login/oauth2/code/github`
3. Сохраните изменения

### Проблема: "Access denied"

**Причина:** Пользователь отклонил авторизацию

**Решение:**
- Повторите авторизацию
- Проверьте scopes в `application.yml` (должны быть `read:user`, `user:email`)

### Проблема: 401 Unauthorized на /api/me

**Причина:** OAuth2 сессия не создана

**Решение:**
```bash
# Сначала выполните OAuth2 login
curl -v -c cookies.txt http://localhost:8080/oauth2/authorization/github

# Затем используйте cookies для /api/me
curl -b cookies.txt http://localhost:8080/api/me
```

---

## 📚 Endpoints

| Endpoint | Описание | Аутентификация |
|----------|----------|----------------|
| `/oauth2/authorization/github` | Начать OAuth2 flow | Нет |
| `/login/oauth2/code/github` | OAuth2 callback | Автоматически |
| `/api/me` | Профиль пользователя | OAuth2 |
| `/api/me/authorities` | Права пользователя | OAuth2 |
| `/api/v1/auth/login` | JWT login | Нет |
| `/api/v1/tournaments` | API турниров | JWT |

---

## 🎯 Интеграция с существующим API

### Вариант 1: OAuth2 → JWT Bridge (будущее)

```java
@PostMapping("/api/oauth2/token")
public AuthResponse convertOAuth2ToJWT(@AuthenticationPrincipal OAuth2User user) {
    // Создать JWT токен на основе OAuth2 пользователя
    // Вернуть AuthResponse с JWT токенами
}
```

### Вариант 2: Dual Authentication (текущее)

- **Web UI**: OAuth2 (session-based)
- **API Clients**: JWT (stateless)
- **Mobile Apps**: JWT (stateless)

---

## ✅ Готово для AI-125

| Требование | Статус | Реализация |
|------------|--------|------------|
| Backend security | ✅ | OAuth2 + JWT |
| Access control | ✅ | Spring Security |
| Development mode | ✅ | GitHub OAuth2 |
| No hardcoded secrets | ✅ | Environment variables |
| MVP ready | ✅ | Готово к тестированию |

**🎉 GitHub OAuth2 аутентификация готова к использованию!**
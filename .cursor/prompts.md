# Cursor AI Prompts for Tournament Service

## ⚠️ TDD WORKFLOW (ОБЯЗАТЕЛЬНО)

При создании ЛЮБОЙ функциональности используй этот порядок:
```
1. СНАЧАЛА напиши Unit-тесты для Service (тесты должны падать)
2. ЗАТЕМ напиши Integration-тесты для Controller (тесты должны падать)
3. ТОЛЬКО ПОТОМ реализуй код (тесты должны проходить)
4. Убедись в 100% покрытии тестами
```

---

## Quick Commands for Cursor Chat/Composer

### 🆕 Create New Endpoint (TDD)
```
Create a new REST endpoint for [description].
⚠️ TDD ORDER:
1. FIRST: Write unit tests for Service (ServiceTest.java)
2. SECOND: Write integration tests for Controller (ControllerTest.java)
3. THIRD: Implement Service method
4. FOURTH: Implement Controller endpoint
5. FIFTH: Add DTO if needed
Include: OpenAPI annotations, 100% test coverage.
```

### 🐛 Fix Bug
```
Fix the bug in [file/method].
Error: [error message]
Expected behavior: [what should happen]
```

### ✅ Add Tests
```
Write unit tests for [Service/Controller].
Use JUnit 5 and Mockito.
Cover: happy path, validation errors, authorization checks.
```

### 📊 Add Migration
```
Create Flyway migration V{N}__ for:
- [describe table/column changes]
Follow existing migration patterns.
```

### 🔒 Add Security Check
```
Add authorization check to [endpoint/method].
Only [ORGANIZER/ADMIN] should be able to perform this action.
Use tournament.isOrganizer(currentUserId) pattern.
```

### 🧪 TDD New Feature (RECOMMENDED)
```
Implement [feature] using TDD:

STEP 1 - Unit Tests (write first, must fail):
- Test happy path
- Test validation errors  
- Test authorization denied
- Test not found scenarios

STEP 2 - Integration Tests (write second, must fail):
- Test HTTP 200/201 success
- Test HTTP 400 validation
- Test HTTP 401 unauthorized
- Test HTTP 403 forbidden
- Test HTTP 404 not found

STEP 3 - Implementation (write last, tests must pass):
- Service method
- Controller endpoint
- DTO if needed

Ensure 100% test coverage.
```

---

## Example Prompts

### Add new field to Tournament
```
Add "location" field to Tournament entity:
- String, max 200 chars, optional
- Include in TournamentRequest and TournamentResponse DTOs
- Create Flyway migration
- Update mapToResponse in TournamentService
```

### Create notification service stub
```
Create AlertService stub for future notifications:
- Interface with methods: notifyTournamentStart, notifyGameResult, notifyWinner
- Empty implementation for MVP
- Add TODO comments for future integration
```

### Add pagination to winners
```
Add pagination to GET /api/tournaments/{id}/games/winners endpoint:
- Use Spring Data Pageable
- Return Page<WinnerResponse>
- Keep existing non-paginated endpoint as /winners/all
```

---

## Context Files to Include

When asking Cursor AI, reference these files:
- `.cursorrules` - Project rules and patterns
- `build.gradle` - Dependencies
- Related entity in `src/main/java/.../entity/`
- Related controller for API patterns
- Existing migrations in `src/main/resources/db/migration/`

---

## Keyboard Shortcuts

| Action | Shortcut |
|--------|----------|
| Open Chat | Cmd/Ctrl + L |
| Open Composer | Cmd/Ctrl + I |
| Inline Edit | Cmd/Ctrl + K |
| Accept suggestion | Tab |
| Reject suggestion | Esc |

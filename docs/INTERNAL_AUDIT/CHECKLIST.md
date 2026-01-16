# Internal Audit Checklist — Tournament Service Backend

## Date: 2026-01-15
## Auditor: Cursor AI

---

## Phase 0: System Map
- [ ] Project structure documented
- [ ] SYSTEM_MAP.md created
- [ ] CHECKLIST.md created

## Phase 1: Port & Docker Cleanup
- [ ] Check port 5432 (PostgreSQL)
- [ ] Check port 8080 (Application)
- [ ] Stop existing Docker containers
- [ ] Verify ports are free

## Phase 2: Docker Build & Run
- [ ] Dockerfile exists and valid
- [ ] docker-compose.yml exists and valid
- [ ] Docker build succeeds
- [ ] Docker compose up succeeds
- [ ] PostgreSQL healthy
- [ ] Application starts

## Phase 3: Compilation & Functional Testing
- [ ] Java 17 installed
- [ ] Gradle wrapper works
- [ ] `./gradlew clean build` succeeds
- [ ] JAR file created
- [ ] Application starts with bootRun
- [ ] Health endpoint returns UP
- [ ] Swagger UI accessible
- [ ] OpenAPI spec accessible
- [ ] Auth endpoints respond correctly
- [ ] Protected endpoints require auth

## Phase 4: Unit & Integration Tests
- [ ] All unit tests pass
- [ ] All integration tests pass
- [ ] Test coverage acceptable (>80%)
- [ ] No skipped tests
- [ ] No flaky tests

## Phase 5: Migrations & Database
- [ ] All migrations present (V1-V9)
- [ ] Migrations in correct order
- [ ] All migrations applied successfully
- [ ] Schema matches entities
- [ ] Indexes created
- [ ] Foreign keys correct

## Phase 6: Security Check
- [ ] No hardcoded passwords in code
- [ ] No hardcoded secrets in code
- [ ] No API keys in code
- [ ] No private keys in repo
- [ ] .env not committed
- [ ] Secrets use environment variables
- [ ] JWT secret externalized
- [ ] Database password externalized

## Phase 7: OpenAPI / Swagger
- [ ] Swagger UI accessible
- [ ] OpenAPI spec valid JSON
- [ ] All endpoints documented
- [ ] Request/Response schemas correct
- [ ] Auth documented

## Phase 8: CI/CD Check
- [ ] GitHub Actions workflow exists
- [ ] Build step configured
- [ ] Test step configured
- [ ] Docker build step configured
- [ ] Deployment step configured (if applicable)

## Phase 9: Additional Checks
- [ ] README.md complete
- [ ] CHANGELOG.md maintained
- [ ] No TODO/FIXME in critical code
- [ ] Logging configured properly
- [ ] Error handling complete
- [ ] No deprecated APIs used
- [ ] Dependencies up to date

---

## Findings Summary

| Severity | Count | Status |
|----------|-------|--------|
| P0 (Critical) | TBD | TBD |
| P1 (High) | TBD | TBD |
| P2 (Medium) | TBD | TBD |
| P3 (Low) | TBD | TBD |

---

## Sign-off

- [ ] All P0 issues resolved
- [ ] All P1 issues resolved or tracked
- [ ] P2 issues documented
- [ ] Release approved / blocked

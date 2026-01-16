# Internal Audit Checklist — Tournament Service Backend

## 📋 Pre-Audit Setup

- [ ] Audit environment prepared
- [ ] All terminals closed/cleaned
- [ ] Docker containers stopped
- [ ] Ports freed (5432, 8080, 8200)
- [ ] Fresh repository state confirmed

---

## 🔍 Phase 0: Repository Snapshot

- [x] Project structure analyzed
- [x] Main modules identified (controller, service, repository, entity, dto, config, security, exception)
- [x] System map generated (`docs/INTERNAL_AUDIT/SYSTEM_MAP.md`)
- [x] Checklist created (`docs/INTERNAL_AUDIT/CHECKLIST.md`)

---

## ⚠️ Phase 1: Port Check & Docker Cleanup (CRITICAL)

### Port Verification
- [ ] PostgreSQL port 5432 checked
- [ ] Application port 8080 checked  
- [ ] Vault port 8200 checked (if used)
- [ ] Redis port 6379 checked (if used)
- [ ] RabbitMQ ports 5672, 15672 checked (if used)

### Docker Cleanup
- [ ] `docker ps` - running containers listed
- [ ] `docker ps -a` - all containers listed
- [ ] `docker compose down` - current project stopped
- [ ] `docker stop $(docker ps -q)` - all containers stopped
- [ ] Specific containers stopped (postgres, tournament-db, etc.)

### Process Cleanup
- [ ] `lsof -i :5432` - PostgreSQL processes killed
- [ ] `lsof -i :8080` - Application processes killed
- [ ] Background jobs terminated
- [ ] Port conflicts resolved

### Cleanup Script Execution
- [ ] Full cleanup script executed
- [ ] All ports confirmed free
- [ ] Ready for fresh start

---

## 🐳 Phase 2: Docker Build & Launch

### Dockerfile Validation
- [ ] `Dockerfile` exists and readable
- [ ] Base image is current (eclipse-temurin:17-jre or similar)
- [ ] Multi-stage build implemented
- [ ] `EXPOSE 8080` declared
- [ ] Non-root user configured
- [ ] `HEALTHCHECK` configured
- [ ] Security best practices followed

### Docker Compose Validation
- [ ] `docker-compose.yml` exists and readable
- [ ] `docker compose config` - syntax valid
- [ ] PostgreSQL service with healthcheck
- [ ] Application service with proper dependencies
- [ ] Volumes configured for data persistence
- [ ] Environment variables via `.env` file
- [ ] `depends_on` with `condition: service_healthy`

### Build Process
- [ ] `docker build -t tournament-service:latest .` - successful
- [ ] `docker images | grep tournament` - image created
- [ ] `docker history tournament-service:latest` - layers inspected
- [ ] Image size reasonable (<500MB)

### Launch Process
- [ ] `docker compose up -d` - services started
- [ ] `docker compose ps` - all services running
- [ ] `docker compose logs` - no critical errors
- [ ] Container health checks passing

### Post-Launch Verification
- [ ] PostgreSQL container healthy
- [ ] Application container healthy
- [ ] Database connectivity verified
- [ ] Flyway migrations applied successfully

---

## 🔨 Phase 3: Compilation & Functional Testing

### Pre-Compilation Checks
- [ ] `java -version` - Java 17+ confirmed
- [ ] `echo $JAVA_HOME` - JAVA_HOME set correctly
- [ ] `./gradlew --version` - Gradle wrapper working
- [ ] `chmod +x gradlew` - permissions set

### Build Process
- [ ] `./gradlew clean` - clean successful
- [ ] `./gradlew compileJava` - compilation successful
- [ ] `./gradlew build -x test` - build without tests successful
- [ ] `./gradlew build` - full build with tests successful

### JAR Verification
- [ ] `ls -la build/libs/` - JAR file created
- [ ] JAR file size reasonable (50-100MB)
- [ ] `jar tf build/libs/*.jar | head -50` - JAR contents inspected

### Application Launch (Non-Docker)
- [ ] PostgreSQL running (Docker or local)
- [ ] `./gradlew bootRun --args='--spring.profiles.active=dev'` - app starts
- [ ] OR `java -jar build/libs/*.jar --spring.profiles.active=dev` - app starts
- [ ] No startup errors in logs
- [ ] Application reaches "Started TournamentServiceBeApplication" state

### Smoke Tests
- [ ] `curl http://localhost:8080/actuator/health` - returns `{"status":"UP"}`
- [ ] `curl http://localhost:8080/actuator/info` - returns app info
- [ ] `curl http://localhost:8080/swagger-ui.html` - returns 200/302
- [ ] `curl http://localhost:8080/v3/api-docs` - returns OpenAPI JSON
- [ ] `curl http://localhost:8080/api/v1/tournaments` - returns 401 (auth required)
- [ ] Database connectivity confirmed via `/actuator/health`

### Authentication Flow Test
- [ ] User registration endpoint accessible
- [ ] User login endpoint accessible  
- [ ] JWT token generation working
- [ ] Protected endpoints require valid JWT
- [ ] Invalid JWT returns 401/403

---

## 🧪 Phase 4: Unit & Integration Tests

### Test Execution
- [ ] `./gradlew test` - all tests executed
- [ ] Test results analyzed
- [ ] Failed tests identified and categorized
- [ ] Test coverage report generated (if available)

### Test Categories
- [ ] **Unit Tests** - Service layer tested
- [ ] **Integration Tests** - Controller layer tested  
- [ ] **Repository Tests** - Data layer tested
- [ ] **Security Tests** - JWT authentication tested
- [ ] **Validation Tests** - Entity validation tested

### Specific Test Suites
- [ ] `TournamentServiceTest` - tournament business logic
- [ ] `GameServiceTest` - game management logic
- [ ] `TournamentPlayerServiceTest` - player management
- [ ] `AuthServiceTest` - authentication logic
- [ ] `JwtTokenProviderTest` - JWT token operations
- [ ] Controller integration tests
- [ ] Repository tests with @DataJpaTest

### Test Results Analysis
- [ ] Total test count documented
- [ ] Pass/fail ratio calculated
- [ ] Failed test root causes identified
- [ ] Performance issues noted
- [ ] Coverage gaps identified

---

## 🗄 Phase 5: Migrations & Database

### Migration Files
- [ ] `ls src/main/resources/db/migration/` - migration files listed
- [ ] Migration naming convention verified (V1__, V2__, etc.)
- [ ] Migration order confirmed
- [ ] No missing versions in sequence

### Applied Migrations
- [ ] `SELECT * FROM flyway_schema_history` - applied migrations listed
- [ ] All migrations successful
- [ ] No checksum mismatches
- [ ] No description mismatches
- [ ] Migration versions match code

### Database Schema
- [ ] `\dt` - all expected tables exist
- [ ] `\d tournaments` - tournament table structure correct
- [ ] `\d users` - users table structure correct
- [ ] `\d games` - games table structure correct
- [ ] `\d tournament_players` - players table structure correct
- [ ] `\d tournament_winners` - winners table structure correct

### Indexes & Constraints
- [ ] Primary keys on all tables
- [ ] Foreign key constraints properly defined
- [ ] Unique constraints where needed
- [ ] Indexes for performance (tournament_id, user_id, etc.)
- [ ] Composite indexes for queries

---

## 🔒 Phase 6: Security Check

### Secret Scanning
- [ ] `grep -rn "password=" --include="*.java" --include="*.yml"` - no hardcoded passwords
- [ ] `grep -rn "secret=" --include="*.java" --include="*.yml"` - no hardcoded secrets
- [ ] `grep -rn "api_key\|apiKey"` - no hardcoded API keys
- [ ] `grep -rn "BEGIN PRIVATE KEY"` - no private keys in code
- [ ] `grep -rn "ghp_\|github_pat_"` - no GitHub tokens

### Configuration Security
- [ ] `.gitignore` includes `.env` files
- [ ] `.env` files not committed to git
- [ ] `application-prod.yml` uses environment variables
- [ ] No default secrets in production config
- [ ] JWT secret externalized

### Authentication Security
- [ ] Password hashing implemented (BCrypt)
- [ ] JWT tokens properly signed
- [ ] Token expiration configured
- [ ] Rate limiting configured
- [ ] CORS properly configured

---

## 📖 Phase 7: OpenAPI / Swagger

### Swagger UI
- [ ] `curl http://localhost:8080/swagger-ui.html` - accessible
- [ ] Swagger UI loads without errors
- [ ] All endpoints documented
- [ ] Authentication schemes documented

### OpenAPI Specification
- [ ] `curl http://localhost:8080/v3/api-docs` - spec available
- [ ] JSON structure valid
- [ ] All endpoints included
- [ ] Request/response schemas defined
- [ ] Authentication requirements specified

### API Documentation Quality
- [ ] Endpoint descriptions present
- [ ] Request/response examples provided
- [ ] Error responses documented
- [ ] Authentication flow explained

---

## 🚀 Phase 8: CI/CD Check

### GitHub Actions
- [ ] `.github/workflows/` directory exists
- [ ] CI workflow defined
- [ ] Build job configured
- [ ] Test job configured
- [ ] Security scanning configured
- [ ] Docker build job configured

### Workflow Quality
- [ ] Java version specified correctly
- [ ] PostgreSQL service configured for tests
- [ ] Environment variables handled securely
- [ ] Artifact upload configured
- [ ] Deployment jobs defined (if applicable)

---

## 📊 Final Compliance Check

### P0 Issues (Blockers)
- [ ] No P0 issues found
- [ ] All P0 issues resolved
- [ ] Application starts successfully
- [ ] Database connectivity working
- [ ] Critical endpoints functional

### P1 Issues (Pre-Release)
- [ ] No P1 security issues
- [ ] No P1 functionality issues
- [ ] All tests passing
- [ ] Documentation complete

### P2 Issues (Post-Release)
- [ ] P2 issues documented
- [ ] Improvement recommendations provided
- [ ] Technical debt identified

### Sign-Off Criteria
- [ ] All P0 issues resolved
- [ ] All P1 issues resolved or accepted
- [ ] Test suite passing
- [ ] Security scan clean
- [ ] Documentation complete
- [ ] Deployment verified

---

## 📝 Audit Artifacts

### Generated Files
- [ ] `docs/INTERNAL_AUDIT/SYSTEM_MAP.md`
- [ ] `docs/INTERNAL_AUDIT/CHECKLIST.md`
- [ ] `docs/INTERNAL_AUDIT/COMPLIANCE_REPORT.md`

### Evidence Files
- [ ] Test execution logs
- [ ] Build logs
- [ ] Docker logs
- [ ] Database schema dumps
- [ ] Security scan results
- [ ] OpenAPI specification

---

*Checklist Version: 2.0*  
*Last Updated: 2026-01-16*  
*Auditor: Cursor AI (Principal Backend Engineer + SRE + Security/Compliance)*
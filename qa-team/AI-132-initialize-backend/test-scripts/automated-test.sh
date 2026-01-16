#!/bin/bash

# Automated Test Script for Tournament Service Backend
# Updated: 2026-01-02 for Gradle migration and v0.7.0 changes
# Usage: ./automated-test.sh [from project root]

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║  Tournament Service BE: Automated Test Suite                 ║"
echo "║  Version: 2.0 (Gradle + Security Fixes)                      ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""
echo "Test Date: $(date)"
echo "Tester: Automated Script"
echo ""

PASSED=0
FAILED=0
EVIDENCE_DIR="qa-team/AI-132-initialize-backend/test-evidence"

# Create evidence directory
mkdir -p "$EVIDENCE_DIR"

# Helper function
test_case() {
    local name=$1
    local command=$2
    echo -n "[$name] "
    if eval "$command" > /dev/null 2>&1; then
        echo "✅ PASS"
        ((PASSED++))
    else
        echo "❌ FAIL"
        ((FAILED++))
    fi
}

# ===========================================
# TEST SUITE 1: Project Structure (Gradle)
# ===========================================
echo "=== TEST SUITE 1: Project Structure (Gradle) ==="
test_case "TC-001: build.gradle exists" "test -f build.gradle"
test_case "TC-002: settings.gradle exists" "test -f settings.gradle"
test_case "TC-003: Gradle wrapper exists" "test -f gradlew"
test_case "TC-004: application.yml exists" "test -f src/main/resources/application.yml"
test_case "TC-005: Main class exists" "test -f src/main/java/com/chessai/tournament/TournamentServiceBeApplication.java"
test_case "TC-006: SecurityConfig exists" "test -f src/main/java/com/chessai/tournament/config/SecurityConfig.java"
test_case "TC-007: Maven files removed" "! test -f pom.xml && ! test -f mvnw"
echo ""

# ===========================================
# TEST SUITE 2: Dependencies (Gradle)
# ===========================================
echo "=== TEST SUITE 2: Dependencies Check (Gradle) ==="
test_case "TC-008: Java 17 configured" "grep -q \"sourceCompatibility = '17'\" build.gradle"
test_case "TC-009: Spring Boot 3.5.9" "grep -q \"3.5.9\" build.gradle"
test_case "TC-010: PostgreSQL dependency" "grep -q 'postgresql' build.gradle"
test_case "TC-011: JPA dependency" "grep -q 'spring-boot-starter-data-jpa' build.gradle"
test_case "TC-012: Security dependency" "grep -q 'spring-boot-starter-security' build.gradle"
test_case "TC-013: Swagger dependency" "grep -q 'springdoc-openapi' build.gradle"
test_case "TC-014: JWT dependency" "grep -q 'jjwt' build.gradle"
test_case "TC-015: Bucket4j dependency" "grep -q 'bucket4j' build.gradle"
test_case "TC-016: Caffeine dependency" "grep -q 'caffeine' build.gradle"
test_case "TC-017: Vault dependency" "grep -q 'vault-config' build.gradle"
echo ""

# ===========================================
# TEST SUITE 3: Configuration
# ===========================================
echo "=== TEST SUITE 3: Configuration Check ==="
test_case "TC-018: PostgreSQL URL configured" "grep -q 'jdbc:postgresql' src/main/resources/application.yml"
test_case "TC-019: JPA configured" "grep -q 'jpa:' src/main/resources/application.yml"
test_case "TC-020: Server port configured" "grep -q 'port:' src/main/resources/application.yml"
test_case "TC-021: JWT configured" "grep -q 'jwt:' src/main/resources/application.yml"
test_case "TC-022: Access token 30 min" "grep -q '1800000' src/main/resources/application.yml"
echo ""

# ===========================================
# TEST SUITE 4: Security Components
# ===========================================
echo "=== TEST SUITE 4: Security Components ==="
test_case "TC-023: AuthController exists" "test -f src/main/java/com/chessai/tournament/controller/AuthController.java"
test_case "TC-024: JwtTokenProvider exists" "test -f src/main/java/com/chessai/tournament/security/jwt/JwtTokenProvider.java"
test_case "TC-025: RateLimitingService exists" "test -f src/main/java/com/chessai/tournament/security/ratelimit/RateLimitingService.java"
test_case "TC-026: User entity exists" "test -f src/main/java/com/chessai/tournament/entity/User.java"
test_case "TC-027: Role enum exists" "test -f src/main/java/com/chessai/tournament/entity/Role.java"
test_case "TC-028: AuthService exists" "test -f src/main/java/com/chessai/tournament/security/service/AuthService.java"
echo ""

# ===========================================
# TEST SUITE 5: Bug Fixes Verification (v0.7.0)
# ===========================================
echo "=== TEST SUITE 5: Bug Fixes Verification (v0.7.0) ==="
test_case "TC-029: Caffeine cache in RateLimiting" "grep -q 'Caffeine' src/main/java/com/chessai/tournament/security/ratelimit/RateLimitingService.java"
test_case "TC-030: Last IP from X-Forwarded-For" "grep -q 'ips.length - 1' src/main/java/com/chessai/tournament/security/ratelimit/RateLimitingFilter.java"
test_case "TC-031: No duplicate addRole" "! grep -q 'user.addRole(Role.USER)' src/main/java/com/chessai/tournament/security/service/AuthService.java"
test_case "TC-032: No @Size on password in User" "! grep -A1 'private String password' src/main/java/com/chessai/tournament/entity/User.java | grep -q '@Size'"
test_case "TC-033: isAccountNonLocked check" "grep -q 'isAccountNonLocked' src/main/java/com/chessai/tournament/security/service/AuthService.java"
test_case "TC-034: instanceof FieldError" "grep -q 'instanceof FieldError' src/main/java/com/chessai/tournament/exception/GlobalExceptionHandler.java"
test_case "TC-035: requireIssuer in JWT" "grep -q 'requireIssuer' src/main/java/com/chessai/tournament/security/jwt/JwtTokenProvider.java"
echo ""

# ===========================================
# TEST SUITE 6: Database Migrations
# ===========================================
echo "=== TEST SUITE 6: Database Migrations ==="
test_case "TC-036: V1 migration exists" "test -f src/main/resources/db/migration/V1__init.sql"
test_case "TC-037: V2 users migration exists" "test -f src/main/resources/db/migration/V2__create_users_table.sql"
test_case "TC-038: Flyway configured" "grep -q 'flyway:' src/main/resources/application.yml"
echo ""

# ===========================================
# TEST SUITE 7: Documentation
# ===========================================
echo "=== TEST SUITE 7: Documentation ==="
test_case "TC-039: README exists" "test -f README.md"
test_case "TC-040: CHANGELOG exists" "test -f CHANGELOG.md"
test_case "TC-041: Gradle build docs" "test -f docs/GRADLE_BUILD_CONFIGURATION.md"
test_case "TC-042: Security setup docs" "test -f docs/SECURITY_SETUP.md"
test_case "TC-043: Bugfixes docs" "test -f docs/BUGFIXES_AND_CAFFEINE_INTEGRATION.md"
test_case "TC-044: Semgrep report" "test -f docs/SEMGREP_SECURITY_REPORT.md"
echo ""

# ===========================================
# TEST SUITE 8: Git Repository
# ===========================================
echo "=== TEST SUITE 8: Git Repository ==="
test_case "TC-045: Git initialized" "test -d .git"
test_case "TC-046: .gitignore exists" "test -f .gitignore"
test_case "TC-047: build/ ignored" "grep -q 'build/' .gitignore"
test_case "TC-048: .gradle/ ignored" "grep -q '.gradle' .gitignore"
echo ""

# ===========================================
# TEST SUITE 9: Build Verification
# ===========================================
echo "=== TEST SUITE 9: Build Verification ==="
echo "Running Gradle compile..."
if ./gradlew compileJava --no-daemon -q 2>/dev/null; then
    echo "[TC-049: Gradle compileJava] ✅ PASS"
    ((PASSED++))
else
    echo "[TC-049: Gradle compileJava] ❌ FAIL"
    ((FAILED++))
fi
echo ""

# Save evidence
echo "Saving test evidence..."
find src -type f -name "*.java" | sort > "$EVIDENCE_DIR/project-structure.txt" 2>/dev/null
grep -E "implementation|runtimeOnly|testImplementation" build.gradle > "$EVIDENCE_DIR/dependencies.txt" 2>/dev/null
echo "Test run: $(date)" > "$EVIDENCE_DIR/last-test-run.txt"

# Summary
echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║  TEST SUMMARY                                                ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""
echo "  Total Passed: $PASSED"
echo "  Total Failed: $FAILED"
echo "  Total Tests:  $((PASSED + FAILED))"
echo ""
if [ $FAILED -eq 0 ]; then
    echo "  Result: ✅ ALL TESTS PASSED"
    echo ""
    exit 0
else
    echo "  Result: ❌ SOME TESTS FAILED"
    echo ""
    exit 1
fi

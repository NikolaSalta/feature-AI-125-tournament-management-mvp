package com.chessai.tournament.security.jwt;

import com.chessai.tournament.entity.Role;
import com.chessai.tournament.entity.User;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Base64;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit-тесты для JwtTokenProvider.
 * Тестирует генерацию, валидацию и парсинг JWT токенов.
 */
@DisplayName("JwtTokenProvider Tests")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private JwtProperties jwtProperties;
    private User testUser;

    // Тестовый Base64-encoded секрет (256 бит = 32 байта)
    private static final String TEST_SECRET = Base64.getEncoder()
            .encodeToString("test-secret-key-for-jwt-tokens-32bytes!".getBytes());

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret(TEST_SECRET);
        jwtProperties.setAccessTokenExpiration(1800000); // 30 минут
        jwtProperties.setRefreshTokenExpiration(604800000); // 7 дней
        jwtProperties.setIssuer("tournament-service");

        jwtTokenProvider = new JwtTokenProvider(jwtProperties);

        // Создаём тестового пользователя
        testUser = new User("testuser", "test@example.com", "password123");
        testUser.setId(1L);
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setRoles(Set.of(Role.USER, Role.ADMIN));
    }

    @Nested
    @DisplayName("Token Generation Tests")
    class TokenGenerationTests {

        @Test
        @DisplayName("Should generate valid access token")
        void shouldGenerateValidAccessToken() {
            // When
            String token = jwtTokenProvider.generateAccessToken(testUser);

            // Then
            assertNotNull(token);
            assertFalse(token.isEmpty());
            assertTrue(token.split("\\.").length == 3); // JWT format: header.payload.signature
        }

        @Test
        @DisplayName("Should generate valid refresh token")
        void shouldGenerateValidRefreshToken() {
            // When
            String token = jwtTokenProvider.generateRefreshToken(testUser);

            // Then
            assertNotNull(token);
            assertFalse(token.isEmpty());
            assertTrue(token.split("\\.").length == 3);
        }

        @Test
        @DisplayName("Access token should contain correct username")
        void accessTokenShouldContainCorrectUsername() {
            // When
            String token = jwtTokenProvider.generateAccessToken(testUser);
            String username = jwtTokenProvider.getUsernameFromToken(token);

            // Then
            assertEquals("testuser", username);
        }

        @Test
        @DisplayName("Refresh token should contain correct username")
        void refreshTokenShouldContainCorrectUsername() {
            // When
            String token = jwtTokenProvider.generateRefreshToken(testUser);
            String username = jwtTokenProvider.getUsernameFromToken(token);

            // Then
            assertEquals("testuser", username);
        }

        @Test
        @DisplayName("Access and refresh tokens should be different")
        void accessAndRefreshTokensShouldBeDifferent() {
            // When
            String accessToken = jwtTokenProvider.generateAccessToken(testUser);
            String refreshToken = jwtTokenProvider.generateRefreshToken(testUser);

            // Then
            assertNotEquals(accessToken, refreshToken);
        }
    }

    @Nested
    @DisplayName("Token Validation Tests")
    class TokenValidationTests {

        @Test
        @DisplayName("Should validate correct access token")
        void shouldValidateCorrectAccessToken() {
            // Given
            String token = jwtTokenProvider.generateAccessToken(testUser);

            // When
            boolean isValid = jwtTokenProvider.validateToken(token);

            // Then
            assertTrue(isValid);
        }

        @Test
        @DisplayName("Should validate correct refresh token")
        void shouldValidateCorrectRefreshToken() {
            // Given
            String token = jwtTokenProvider.generateRefreshToken(testUser);

            // When
            boolean isValid = jwtTokenProvider.validateToken(token);

            // Then
            assertTrue(isValid);
        }

        @Test
        @DisplayName("Should reject malformed token")
        void shouldRejectMalformedToken() {
            // Given
            String malformedToken = "not.a.valid.jwt.token";

            // When
            boolean isValid = jwtTokenProvider.validateToken(malformedToken);

            // Then
            assertFalse(isValid);
        }

        @Test
        @DisplayName("Should reject empty token")
        void shouldRejectEmptyToken() {
            // When
            boolean isValid = jwtTokenProvider.validateToken("");

            // Then
            assertFalse(isValid);
        }

        @Test
        @DisplayName("Should reject token with invalid signature")
        void shouldRejectTokenWithInvalidSignature() {
            // Given
            String token = jwtTokenProvider.generateAccessToken(testUser);
            // Изменяем последний символ подписи
            String tamperedToken = token.substring(0, token.length() - 1) + "X";

            // When
            boolean isValid = jwtTokenProvider.validateToken(tamperedToken);

            // Then
            assertFalse(isValid);
        }
    }

    @Nested
    @DisplayName("Token Type Tests")
    class TokenTypeTests {

        @Test
        @DisplayName("Should correctly identify access token")
        void shouldCorrectlyIdentifyAccessToken() {
            // Given
            String accessToken = jwtTokenProvider.generateAccessToken(testUser);

            // When/Then
            assertTrue(jwtTokenProvider.isAccessToken(accessToken));
            assertFalse(jwtTokenProvider.isRefreshToken(accessToken));
        }

        @Test
        @DisplayName("Should correctly identify refresh token")
        void shouldCorrectlyIdentifyRefreshToken() {
            // Given
            String refreshToken = jwtTokenProvider.generateRefreshToken(testUser);

            // When/Then
            assertTrue(jwtTokenProvider.isRefreshToken(refreshToken));
            assertFalse(jwtTokenProvider.isAccessToken(refreshToken));
        }

        @Test
        @DisplayName("Should return correct token type from claims")
        void shouldReturnCorrectTokenType() {
            // Given
            String accessToken = jwtTokenProvider.generateAccessToken(testUser);
            String refreshToken = jwtTokenProvider.generateRefreshToken(testUser);

            // When/Then
            assertEquals("access", jwtTokenProvider.getTokenType(accessToken));
            assertEquals("refresh", jwtTokenProvider.getTokenType(refreshToken));
        }
    }

    @Nested
    @DisplayName("Token Expiration Tests")
    class TokenExpirationTests {

        @Test
        @DisplayName("Should return expiration date for valid token")
        void shouldReturnExpirationDate() {
            // Given
            String token = jwtTokenProvider.generateAccessToken(testUser);

            // When
            var expiration = jwtTokenProvider.getExpirationFromToken(token);

            // Then
            assertNotNull(expiration);
            assertTrue(expiration.getTime() > System.currentTimeMillis());
        }

        @Test
        @DisplayName("Access token should expire after configured time")
        void accessTokenShouldExpireAfterConfiguredTime() {
            // Given
            String token = jwtTokenProvider.generateAccessToken(testUser);
            var expiration = jwtTokenProvider.getExpirationFromToken(token);

            // Then
            long expectedExpirationTime = System.currentTimeMillis() + jwtProperties.getAccessTokenExpiration();
            // Допускаем погрешность в 5 секунд
            assertTrue(Math.abs(expiration.getTime() - expectedExpirationTime) < 5000);
        }

        @Test
        @DisplayName("Refresh token should have longer expiration than access token")
        void refreshTokenShouldHaveLongerExpiration() {
            // Given
            String accessToken = jwtTokenProvider.generateAccessToken(testUser);
            String refreshToken = jwtTokenProvider.generateRefreshToken(testUser);

            // When
            var accessExpiration = jwtTokenProvider.getExpirationFromToken(accessToken);
            var refreshExpiration = jwtTokenProvider.getExpirationFromToken(refreshToken);

            // Then
            assertTrue(refreshExpiration.after(accessExpiration));
        }

        @Test
        @DisplayName("Should reject expired token")
        void shouldRejectExpiredToken() {
            // Создаём JwtTokenProvider с очень коротким временем жизни
            JwtProperties shortLivedProps = new JwtProperties();
            shortLivedProps.setSecret(TEST_SECRET);
            shortLivedProps.setAccessTokenExpiration(1); // 1 миллисекунда
            shortLivedProps.setRefreshTokenExpiration(1);
            shortLivedProps.setIssuer("tournament-service");

            JwtTokenProvider shortLivedProvider = new JwtTokenProvider(shortLivedProps);
            String token = shortLivedProvider.generateAccessToken(testUser);

            // Ждём истечения токена
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // When
            boolean isValid = shortLivedProvider.validateToken(token);

            // Then
            assertFalse(isValid);
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle user with single role")
        void shouldHandleUserWithSingleRole() {
            // Given
            User singleRoleUser = new User("user", "user@test.com", "pass");
            singleRoleUser.setRoles(Set.of(Role.USER));

            // When
            String token = jwtTokenProvider.generateAccessToken(singleRoleUser);

            // Then
            assertTrue(jwtTokenProvider.validateToken(token));
            assertEquals("user", jwtTokenProvider.getUsernameFromToken(token));
        }

        @Test
        @DisplayName("Should handle user with multiple roles")
        void shouldHandleUserWithMultipleRoles() {
            // Given
            User multiRoleUser = new User("admin", "admin@test.com", "pass");
            multiRoleUser.setRoles(Set.of(Role.USER, Role.ORGANIZER, Role.ADMIN));

            // When
            String token = jwtTokenProvider.generateAccessToken(multiRoleUser);

            // Then
            assertTrue(jwtTokenProvider.validateToken(token));
        }

        @Test
        @DisplayName("Should handle special characters in username")
        void shouldHandleSpecialCharactersInUsername() {
            // Given
            User specialUser = new User("user.name_123", "special@test.com", "pass");

            // When
            String token = jwtTokenProvider.generateAccessToken(specialUser);
            String extractedUsername = jwtTokenProvider.getUsernameFromToken(token);

            // Then
            assertEquals("user.name_123", extractedUsername);
        }
    }
}


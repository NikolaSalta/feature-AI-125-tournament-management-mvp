package com.chessai.tournament.controller;

import com.chessai.tournament.dto.auth.LoginRequest;
import com.chessai.tournament.dto.auth.RefreshTokenRequest;
import com.chessai.tournament.dto.auth.RegisterRequest;
import com.chessai.tournament.entity.Role;
import com.chessai.tournament.entity.User;
import com.chessai.tournament.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Интеграционные тесты для AuthController.
 * Тестирует полный цикл аутентификации через HTTP API.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@org.springframework.test.context.ActiveProfiles("test")
@DisplayName("AuthController Integration Tests")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String AUTH_BASE_URL = "/api/v1/auth";

    @BeforeEach
    void setUp() {
        // Очистка тестовых данных (кроме admin)
        userRepository.findByUsername("testuser").ifPresent(userRepository::delete);
        userRepository.findByUsername("newuser").ifPresent(userRepository::delete);
    }

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class RegisterTests {

        @Test
        @DisplayName("Should register new user and return tokens")
        void shouldRegisterNewUserAndReturnTokens() throws Exception {
            RegisterRequest request = new RegisterRequest();
            request.setUsername("newuser");
            request.setEmail("newuser@example.com");
            request.setPassword("Password123!");
            request.setFirstName("New");
            request.setLastName("User");

            mockMvc.perform(post(AUTH_BASE_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.expiresIn").isNumber())
                    .andExpect(jsonPath("$.user.username").value("newuser"))
                    .andExpect(jsonPath("$.user.email").value("newuser@example.com"))
                    .andExpect(jsonPath("$.user.fullName").value("New User"))
                    .andExpect(jsonPath("$.user.roles", hasItem("USER")));
        }

        @Test
        @DisplayName("Should return 400 for invalid email")
        void shouldReturn400ForInvalidEmail() throws Exception {
            RegisterRequest request = new RegisterRequest();
            request.setUsername("newuser");
            request.setEmail("invalid-email");
            request.setPassword("Password123!");
            request.setFirstName("New");
            request.setLastName("User");

            mockMvc.perform(post(AUTH_BASE_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 for short password")
        void shouldReturn400ForShortPassword() throws Exception {
            RegisterRequest request = new RegisterRequest();
            request.setUsername("newuser");
            request.setEmail("newuser@example.com");
            request.setPassword("short");
            request.setFirstName("New");
            request.setLastName("User");

            mockMvc.perform(post(AUTH_BASE_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 for missing required fields")
        void shouldReturn400ForMissingRequiredFields() throws Exception {
            RegisterRequest request = new RegisterRequest();
            // Не заполняем обязательные поля

            mockMvc.perform(post(AUTH_BASE_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 409 for duplicate username")
        void shouldReturn409ForDuplicateUsername() throws Exception {
            // Создаём существующего пользователя
            createTestUser("existinguser", "existing@example.com");

            RegisterRequest request = new RegisterRequest();
            request.setUsername("existinguser");
            request.setEmail("new@example.com");
            request.setPassword("Password123!");
            request.setFirstName("New");
            request.setLastName("User");

            mockMvc.perform(post(AUTH_BASE_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class LoginTests {

        @Test
        @DisplayName("Should login with username and return tokens")
        void shouldLoginWithUsernameAndReturnTokens() throws Exception {
            createTestUser("testuser", "test@example.com");

            LoginRequest request = new LoginRequest();
            request.setUsernameOrEmail("testuser");
            request.setPassword("Password123!");

            mockMvc.perform(post(AUTH_BASE_URL + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.user.username").value("testuser"));
        }

        @Test
        @DisplayName("Should login with email and return tokens")
        void shouldLoginWithEmailAndReturnTokens() throws Exception {
            createTestUser("testuser", "test@example.com");

            LoginRequest request = new LoginRequest();
            request.setUsernameOrEmail("test@example.com");
            request.setPassword("Password123!");

            mockMvc.perform(post(AUTH_BASE_URL + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty());
        }

        @Test
        @DisplayName("Should return 401 for invalid password")
        void shouldReturn401ForInvalidPassword() throws Exception {
            createTestUser("testuser", "test@example.com");

            LoginRequest request = new LoginRequest();
            request.setUsernameOrEmail("testuser");
            request.setPassword("WrongPassword!");

            mockMvc.perform(post(AUTH_BASE_URL + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 401 for non-existent user")
        void shouldReturn401ForNonExistentUser() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setUsernameOrEmail("nonexistent");
            request.setPassword("Password123!");

            mockMvc.perform(post(AUTH_BASE_URL + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class RefreshTokenTests {

        @Test
        @DisplayName("Should refresh token successfully")
        void shouldRefreshTokenSuccessfully() throws Exception {
            // Сначала регистрируем пользователя и получаем токены
            RegisterRequest registerRequest = new RegisterRequest();
            registerRequest.setUsername("refreshuser");
            registerRequest.setEmail("refresh@example.com");
            registerRequest.setPassword("Password123!");
            registerRequest.setFirstName("Refresh");
            registerRequest.setLastName("User");

            MvcResult registerResult = mockMvc.perform(post(AUTH_BASE_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isCreated())
                    .andReturn();

            // Извлекаем refresh token из ответа
            String responseContent = registerResult.getResponse().getContentAsString();
            JsonNode jsonNode = objectMapper.readTree(responseContent);
            String refreshToken = jsonNode.get("refreshToken").asText();

            // Используем refresh token для получения новых токенов
            RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
            refreshRequest.setRefreshToken(refreshToken);

            mockMvc.perform(post(AUTH_BASE_URL + "/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refreshRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty());
        }

        @Test
        @DisplayName("Should return 401 for invalid refresh token")
        void shouldReturn401ForInvalidRefreshToken() throws Exception {
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("invalid.refresh.token");

            mockMvc.perform(post(AUTH_BASE_URL + "/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/auth/me")
    class GetCurrentUserTests {

        @Test
        @DisplayName("Should return current user info with valid token")
        void shouldReturnCurrentUserInfoWithValidToken() throws Exception {
            // Регистрируем пользователя
            RegisterRequest registerRequest = new RegisterRequest();
            registerRequest.setUsername("meuser");
            registerRequest.setEmail("me@example.com");
            registerRequest.setPassword("Password123!");
            registerRequest.setFirstName("Me");
            registerRequest.setLastName("User");

            MvcResult registerResult = mockMvc.perform(post(AUTH_BASE_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isCreated())
                    .andReturn();

            String responseContent = registerResult.getResponse().getContentAsString();
            JsonNode jsonNode = objectMapper.readTree(responseContent);
            String accessToken = jsonNode.get("accessToken").asText();

            // Получаем информацию о текущем пользователе
            mockMvc.perform(get(AUTH_BASE_URL + "/me")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("meuser"))
                    .andExpect(jsonPath("$.email").value("me@example.com"))
                    .andExpect(jsonPath("$.fullName").value("Me User"));
        }

        @Test
        @DisplayName("Should return 401 without token")
        void shouldReturn401WithoutToken() throws Exception {
            mockMvc.perform(get(AUTH_BASE_URL + "/me"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 401 with invalid token")
        void shouldReturn401WithInvalidToken() throws Exception {
            mockMvc.perform(get(AUTH_BASE_URL + "/me")
                            .header("Authorization", "Bearer invalid.token.here"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/auth/validate")
    class ValidateTokenTests {

        @Test
        @DisplayName("Should validate token successfully")
        void shouldValidateTokenSuccessfully() throws Exception {
            // Регистрируем пользователя
            RegisterRequest registerRequest = new RegisterRequest();
            registerRequest.setUsername("validateuser");
            registerRequest.setEmail("validate@example.com");
            registerRequest.setPassword("Password123!");
            registerRequest.setFirstName("Validate");
            registerRequest.setLastName("User");

            MvcResult registerResult = mockMvc.perform(post(AUTH_BASE_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isCreated())
                    .andReturn();

            String responseContent = registerResult.getResponse().getContentAsString();
            JsonNode jsonNode = objectMapper.readTree(responseContent);
            String accessToken = jsonNode.get("accessToken").asText();

            // Валидируем токен
            mockMvc.perform(get(AUTH_BASE_URL + "/validate")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid").value(true))
                    .andExpect(jsonPath("$.username").value("validateuser"));
        }
    }

    // =====================
    // Helper Methods
    // =====================

    private User createTestUser(String username, String email) {
        User user = new User(username, email, passwordEncoder.encode("Password123!"));
        user.setFirstName("Test");
        user.setLastName("User");
        // Используем HashSet вместо immutable Set.of()
        user.setRoles(new java.util.HashSet<>(Set.of(Role.USER)));
        return userRepository.save(user);
    }
}


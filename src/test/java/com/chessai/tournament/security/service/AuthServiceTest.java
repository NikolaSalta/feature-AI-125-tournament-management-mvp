package com.chessai.tournament.security.service;

import com.chessai.tournament.dto.auth.AuthResponse;
import com.chessai.tournament.dto.auth.LoginRequest;
import com.chessai.tournament.dto.auth.RefreshTokenRequest;
import com.chessai.tournament.dto.auth.RegisterRequest;
import com.chessai.tournament.entity.Role;
import com.chessai.tournament.entity.User;
import com.chessai.tournament.exception.AuthException;
import com.chessai.tournament.repository.UserRepository;
import com.chessai.tournament.security.jwt.JwtProperties;
import com.chessai.tournament.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Unit-тесты для AuthService.
 * Тестирует регистрацию, аутентификацию и обновление токенов.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        // Тестовый пользователь
        testUser = new User("testuser", "test@example.com", "encodedPassword");
        testUser.setId(1L);
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setRoles(Set.of(Role.USER));

        // Запрос на регистрацию
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setEmail("newuser@example.com");
        registerRequest.setPassword("Password123!");
        registerRequest.setFirstName("New");
        registerRequest.setLastName("User");

        // Запрос на вход
        loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail("testuser");
        loginRequest.setPassword("password123");
    }

    @Nested
    @DisplayName("Registration Tests")
    class RegistrationTests {

        @Test
        @DisplayName("Should register new user successfully")
        void shouldRegisterNewUserSuccessfully() {
            // Given
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User savedUser = invocation.getArgument(0);
                savedUser.setId(1L);
                return savedUser;
            });
            when(jwtTokenProvider.generateAccessToken(any(UserDetails.class))).thenReturn("access-token");
            when(jwtTokenProvider.generateRefreshToken(any(UserDetails.class))).thenReturn("refresh-token");
            when(jwtProperties.getAccessTokenExpiration()).thenReturn(1800000L);

            // When
            AuthResponse response = authService.register(registerRequest);

            // Then
            assertNotNull(response);
            assertEquals("access-token", response.getAccessToken());
            assertEquals("refresh-token", response.getRefreshToken());
            assertEquals("Bearer", response.getTokenType());

            // Проверяем, что пользователь был сохранён
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when username already exists")
        void shouldThrowExceptionWhenUsernameExists() {
            // Given
            when(userRepository.existsByUsername("newuser")).thenReturn(true);

            // When/Then
            AuthException exception = assertThrows(AuthException.class,
                    () -> authService.register(registerRequest));

            assertEquals("Username already exists", exception.getMessage());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when email already exists")
        void shouldThrowExceptionWhenEmailExists() {
            // Given
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail("newuser@example.com")).thenReturn(true);

            // When/Then
            AuthException exception = assertThrows(AuthException.class,
                    () -> authService.register(registerRequest));

            assertEquals("Email already exists", exception.getMessage());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should encode password before saving")
        void shouldEncodePasswordBeforeSaving() {
            // Given
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode("Password123!")).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
            when(jwtTokenProvider.generateAccessToken(any(UserDetails.class))).thenReturn("token");
            when(jwtTokenProvider.generateRefreshToken(any(UserDetails.class))).thenReturn("token");
            when(jwtProperties.getAccessTokenExpiration()).thenReturn(1800000L);

            // When
            authService.register(registerRequest);

            // Then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertEquals("encodedPassword", userCaptor.getValue().getPassword());
        }

        @Test
        @DisplayName("Should assign USER role by default")
        void shouldAssignUserRoleByDefault() {
            // Given
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
            when(jwtTokenProvider.generateAccessToken(any(UserDetails.class))).thenReturn("token");
            when(jwtTokenProvider.generateRefreshToken(any(UserDetails.class))).thenReturn("token");
            when(jwtProperties.getAccessTokenExpiration()).thenReturn(1800000L);

            // When
            authService.register(registerRequest);

            // Then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertTrue(userCaptor.getValue().getRoles().contains(Role.USER));
        }
    }

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should login user successfully")
        void shouldLoginUserSuccessfully() {
            // Given
            Authentication authentication = mock(Authentication.class);
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(jwtTokenProvider.generateAccessToken(any(UserDetails.class))).thenReturn("access-token");
            when(jwtTokenProvider.generateRefreshToken(any(UserDetails.class))).thenReturn("refresh-token");
            when(jwtProperties.getAccessTokenExpiration()).thenReturn(1800000L);

            // When
            AuthResponse response = authService.login(loginRequest);

            // Then
            assertNotNull(response);
            assertEquals("access-token", response.getAccessToken());
            assertEquals("refresh-token", response.getRefreshToken());
            assertNotNull(response.getUser());
            assertEquals("testuser", response.getUser().getUsername());
        }

        @Test
        @DisplayName("Should throw exception for invalid credentials")
        void shouldThrowExceptionForInvalidCredentials() {
            // Given
            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            // When/Then
            AuthException exception = assertThrows(AuthException.class,
                    () -> authService.login(loginRequest));

            assertEquals("Invalid username/email or password", exception.getMessage());
        }

        @Test
        @DisplayName("Should update last login time")
        void shouldUpdateLastLoginTime() {
            // Given
            Authentication authentication = mock(Authentication.class);
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(authenticationManager.authenticate(any())).thenReturn(authentication);
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(jwtTokenProvider.generateAccessToken(any(UserDetails.class))).thenReturn("token");
            when(jwtTokenProvider.generateRefreshToken(any(UserDetails.class))).thenReturn("token");
            when(jwtProperties.getAccessTokenExpiration()).thenReturn(1800000L);

            // When
            authService.login(loginRequest);

            // Then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertNotNull(userCaptor.getValue().getLastLogin());
        }
    }

    @Nested
    @DisplayName("Token Refresh Tests")
    class TokenRefreshTests {

        @Test
        @DisplayName("Should refresh token successfully")
        void shouldRefreshTokenSuccessfully() {
            // Given
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("valid-refresh-token");

            when(jwtTokenProvider.validateToken("valid-refresh-token")).thenReturn(true);
            when(jwtTokenProvider.isRefreshToken("valid-refresh-token")).thenReturn(true);
            when(jwtTokenProvider.getUsernameFromToken("valid-refresh-token")).thenReturn("testuser");
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(jwtTokenProvider.generateAccessToken(any(UserDetails.class))).thenReturn("new-access-token");
            when(jwtTokenProvider.generateRefreshToken(any(UserDetails.class))).thenReturn("new-refresh-token");
            when(jwtProperties.getAccessTokenExpiration()).thenReturn(1800000L);

            // When
            AuthResponse response = authService.refreshToken(request);

            // Then
            assertNotNull(response);
            assertEquals("new-access-token", response.getAccessToken());
            assertEquals("new-refresh-token", response.getRefreshToken());
        }

        @Test
        @DisplayName("Should throw exception for invalid refresh token")
        void shouldThrowExceptionForInvalidRefreshToken() {
            // Given
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("invalid-token");

            when(jwtTokenProvider.validateToken("invalid-token")).thenReturn(false);

            // When/Then
            AuthException exception = assertThrows(AuthException.class,
                    () -> authService.refreshToken(request));

            assertEquals("Invalid refresh token", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when using access token as refresh token")
        void shouldThrowExceptionWhenUsingAccessTokenAsRefreshToken() {
            // Given
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("access-token");

            when(jwtTokenProvider.validateToken("access-token")).thenReturn(true);
            when(jwtTokenProvider.isRefreshToken("access-token")).thenReturn(false);

            // When/Then
            AuthException exception = assertThrows(AuthException.class,
                    () -> authService.refreshToken(request));

            assertEquals("Token is not a refresh token", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("valid-token");

            when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
            when(jwtTokenProvider.isRefreshToken("valid-token")).thenReturn(true);
            when(jwtTokenProvider.getUsernameFromToken("valid-token")).thenReturn("unknown");
            when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

            // When/Then
            AuthException exception = assertThrows(AuthException.class,
                    () -> authService.refreshToken(request));

            assertEquals("User not found", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when user account is disabled")
        void shouldThrowExceptionWhenUserAccountIsDisabled() {
            // Given
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("valid-token");

            testUser.setEnabled(false);

            when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
            when(jwtTokenProvider.isRefreshToken("valid-token")).thenReturn(true);
            when(jwtTokenProvider.getUsernameFromToken("valid-token")).thenReturn("testuser");
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            // When/Then
            AuthException exception = assertThrows(AuthException.class,
                    () -> authService.refreshToken(request));

            assertEquals("User account is disabled", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when user account is locked")
        void shouldThrowExceptionWhenUserAccountIsLocked() {
            // Given
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("valid-token");

            testUser.setAccountNonLocked(false);

            when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
            when(jwtTokenProvider.isRefreshToken("valid-token")).thenReturn(true);
            when(jwtTokenProvider.getUsernameFromToken("valid-token")).thenReturn("testuser");
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            // When/Then
            AuthException exception = assertThrows(AuthException.class,
                    () -> authService.refreshToken(request));

            assertEquals("User account is locked", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("AuthResponse Tests")
    class AuthResponseTests {

        @Test
        @DisplayName("Should return correct user info in response")
        void shouldReturnCorrectUserInfoInResponse() {
            // Given
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(1L);
                return user;
            });
            when(jwtTokenProvider.generateAccessToken(any(UserDetails.class))).thenReturn("token");
            when(jwtTokenProvider.generateRefreshToken(any(UserDetails.class))).thenReturn("token");
            when(jwtProperties.getAccessTokenExpiration()).thenReturn(1800000L);

            // When
            AuthResponse response = authService.register(registerRequest);

            // Then
            assertNotNull(response.getUser());
            assertEquals("newuser", response.getUser().getUsername());
            assertEquals("newuser@example.com", response.getUser().getEmail());
            assertEquals("New User", response.getUser().getFullName());
            assertTrue(response.getUser().getRoles().contains("USER"));
        }

        @Test
        @DisplayName("Should return correct expiration time in seconds")
        void shouldReturnCorrectExpirationTimeInSeconds() {
            // Given
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
            when(jwtTokenProvider.generateAccessToken(any(UserDetails.class))).thenReturn("token");
            when(jwtTokenProvider.generateRefreshToken(any(UserDetails.class))).thenReturn("token");
            when(jwtProperties.getAccessTokenExpiration()).thenReturn(1800000L); // 30 минут в мс

            // When
            AuthResponse response = authService.register(registerRequest);

            // Then
            assertEquals(1800, response.getExpiresIn()); // 30 минут в секундах
        }
    }
}


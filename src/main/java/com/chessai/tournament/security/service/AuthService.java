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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * Сервис аутентификации и авторизации.
 * 
 * Отвечает за:
 * - Регистрацию новых пользователей
 * - Вход в систему (login)
 * - Обновление токенов (refresh)
 * - Выход из системы (logout)
 */
@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final AuthenticationManager authenticationManager;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            JwtProperties jwtProperties,
            AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtProperties = jwtProperties;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Регистрация нового пользователя
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        logger.info("Registering new user: {}", request.getUsername());

        // Проверяем уникальность username
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AuthException("Username already exists");
        }

        // Проверяем уникальность email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AuthException("Email already exists");
        }

        // Создаем пользователя (конструктор уже добавляет Role.USER)
        User user = new User(
                request.getUsername(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        // Role.USER уже добавлена в конструкторе User

        user = userRepository.save(user);
        logger.info("User registered successfully: {}", user.getUsername());

        // Генерируем токены
        return createAuthResponse(user);
    }

    /**
     * Вход в систему
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        logger.info("Login attempt for: {}", request.getUsernameOrEmail());

        try {
            // Аутентификация через Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsernameOrEmail(),
                            request.getPassword()));

            User user = (User) authentication.getPrincipal();

            // Обновляем время последнего входа
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            logger.info("User logged in successfully: {}", user.getUsername());
            return createAuthResponse(user);

        } catch (BadCredentialsException e) {
            logger.warn("Invalid credentials for: {}", request.getUsernameOrEmail());
            throw new AuthException("Invalid username/email or password");
        }
    }

    /**
     * Обновление access-токена с помощью refresh-токена
     */
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        // Валидируем refresh-токен
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new AuthException("Invalid refresh token");
        }

        // Проверяем, что это refresh-токен
        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new AuthException("Token is not a refresh token");
        }

        // Извлекаем username
        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AuthException("User not found"));

        // SECURITY: Проверяем ПОЛНЫЙ статус пользователя (все 4 условия)
        if (!user.isEnabled()) {
            throw new AuthException("User account is disabled");
        }
        if (!user.isAccountNonLocked()) {
            throw new AuthException("User account is locked");
        }
        if (!user.isAccountNonExpired()) {
            throw new AuthException("User account is expired");
        }
        if (!user.isCredentialsNonExpired()) {
            throw new AuthException("User credentials are expired");
        }

        logger.info("Token refreshed for user: {}", username);
        return createAuthResponse(user);
    }

    /**
     * Создает ответ с токенами и информацией о пользователе
     */
    private AuthResponse createAuthResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRoles().stream()
                        .map(Role::name)
                        .collect(Collectors.toSet()));

        return new AuthResponse(
                accessToken,
                refreshToken,
                jwtProperties.getAccessTokenExpiration() / 1000, // в секундах
                userInfo);
    }
}



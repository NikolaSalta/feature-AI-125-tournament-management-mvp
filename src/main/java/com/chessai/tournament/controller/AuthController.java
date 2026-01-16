package com.chessai.tournament.controller;

import com.chessai.tournament.config.ApiVersion;
import com.chessai.tournament.dto.auth.AuthResponse;
import com.chessai.tournament.dto.auth.LoginRequest;
import com.chessai.tournament.dto.auth.RefreshTokenRequest;
import com.chessai.tournament.dto.auth.RegisterRequest;
import com.chessai.tournament.entity.User;
import com.chessai.tournament.security.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Контроллер аутентификации (API v1).
 * 
 * Endpoints:
 * - POST /api/v1/auth/register - регистрация
 * - POST /api/v1/auth/login - вход
 * - POST /api/v1/auth/refresh - обновление токена
 * - GET /api/v1/auth/me - информация о текущем пользователе
 */
@RestController
@RequestMapping(ApiVersion.V1_AUTH)
@Tag(name = "Authentication", description = "API v1: Endpoints for user authentication and authorization")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Регистрация нового пользователя
     */
    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user account and returns JWT tokens")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "409", description = "Username or email already exists")
    })
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Вход в систему
     */
    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticates user and returns JWT tokens")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Обновление access-токена
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Generates a new access token using refresh token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Получение информации о текущем пользователе
     */
    @GetMapping("/me")
    @Operation(summary = "Get current user info", description = "Returns information about the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User info retrieved"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<Map<String, Object>> getCurrentUser(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("fullName", user.getFullName());
        response.put("roles", user.getRoles());
        response.put("createdAt", user.getCreatedAt());
        response.put("lastLogin", user.getLastLogin());

        return ResponseEntity.ok(response);
    }

    /**
     * Проверка валидности токена
     */
    @GetMapping("/validate")
    @Operation(summary = "Validate token", description = "Checks if the current token is valid")
    public ResponseEntity<Map<String, Object>> validateToken(@AuthenticationPrincipal User user) {
        if (user == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("valid", false);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("valid", true);
        response.put("username", user.getUsername());

        return ResponseEntity.ok(response);
    }
}

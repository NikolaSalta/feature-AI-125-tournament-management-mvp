package com.chessai.tournament.config;

import com.chessai.tournament.security.jwt.JwtAuthenticationEntryPoint;
import com.chessai.tournament.security.jwt.JwtAuthenticationFilter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Тестовая конфигурация Security.
 * 
 * Исправляет проблемы с DirtiesContext в интеграционных тестах:
 * - Корректно разделяет публичные и защищённые auth endpoints
 * - /me и /logout требуют аутентификации
 * - /login, /register, /refresh, /validate публичные
 */
@TestConfiguration
@Profile("test")
public class TestSecurityConfig {

    private static final String[] PUBLIC_AUTH_ENDPOINTS = {
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/validate"
    };

    private static final String[] PUBLIC_ENDPOINTS = {
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/actuator/**",
            "/error",
            "/h2-console/**",
            "/api/tournaments/public/**"
    };

    private static final String[] PROTECTED_AUTH_ENDPOINTS = {
            "/api/v1/auth/me",
            "/api/v1/auth/logout"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final AuthenticationProvider authenticationProvider;

    public TestSecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
            AuthenticationProvider authenticationProvider
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.authenticationProvider = authenticationProvider;
    }

    @Bean
    @Primary
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.disable())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .exceptionHandling(exception -> exception
                    .authenticationEntryPoint(jwtAuthenticationEntryPoint)
            )
            .authorizeHttpRequests(auth -> auth
                    // Публичные auth endpoints
                    .requestMatchers(PUBLIC_AUTH_ENDPOINTS).permitAll()
                    // Остальные публичные
                    .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                    // Auth endpoints требующие JWT
                    .requestMatchers(PROTECTED_AUTH_ENDPOINTS).authenticated()
                    // Всё остальное требует аутентификации
                    .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }
}


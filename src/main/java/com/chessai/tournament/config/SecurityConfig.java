package com.chessai.tournament.config;

import com.chessai.tournament.security.jwt.JwtAuthenticationEntryPoint;
import com.chessai.tournament.security.jwt.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Конфигурация Spring Security.
 * 
 * Поддерживает два профиля:
 * - dev: упрощенная безопасность для разработки
 * - prod: полноценная JWT-аутентификация для production
 * 
 * По умолчанию (без профиля) используется production конфигурация.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)  // Включает @PreAuthorize, @PostAuthorize
public class SecurityConfig {

    // =====================
    // Публичные endpoints
    // =====================
    
    // Публичные auth endpoints (без JWT)
    private static final String[] PUBLIC_AUTH_ENDPOINTS = {
            ApiVersion.V1_AUTH + "/register",
            ApiVersion.V1_AUTH + "/login",
            ApiVersion.V1_AUTH + "/refresh",
            ApiVersion.V1_AUTH + "/validate"
    };
    
    // Остальные публичные endpoints
    private static final String[] PUBLIC_ENDPOINTS = {
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/actuator/health",
            "/actuator/info",
            "/error",
            "/api/tournaments/public/**"
    };
    
    // Auth endpoints требующие аутентификации
    private static final String[] AUTH_PROTECTED_ENDPOINTS = {
            ApiVersion.V1_AUTH + "/me",
            ApiVersion.V1_AUTH + "/logout"
    };

    private static final String[] ADMIN_ENDPOINTS = {
            ApiVersion.V1_ADMIN + "/**",
            "/actuator/**"
    };

    // =====================
    // Beans (общие для всех профилей)
    // =====================

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);  // strength = 12 (рекомендуется)
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(passwordEncoder);
        provider.setUserDetailsService(userDetailsService);
        return provider;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:5173",
                "http://localhost:8080"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin"
        ));
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "X-RateLimit-Limit",
                "X-RateLimit-Remaining"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // =====================
    // Production Configuration
    // =====================

    @Configuration
    @Profile({"prod", "default", "test"})  // Активируется для production, default или test профиля
    @Order(2) // Lower priority than OAuth2SecurityConfig
    public static class ProductionSecurityConfig {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
        private final AuthenticationProvider authenticationProvider;
        private final CorsConfigurationSource corsConfigurationSource;

        public ProductionSecurityConfig(
                JwtAuthenticationFilter jwtAuthenticationFilter,
                JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                AuthenticationProvider authenticationProvider,
                CorsConfigurationSource corsConfigurationSource
        ) {
            this.jwtAuthenticationFilter = jwtAuthenticationFilter;
            this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
            this.authenticationProvider = authenticationProvider;
            this.corsConfigurationSource = corsConfigurationSource;
        }

        @Bean
        public SecurityFilterChain productionSecurityFilterChain(HttpSecurity http) throws Exception {
            http
                // Apply to API endpoints only (exclude OAuth2 endpoints)
                .securityMatcher("/api/**", "/actuator/**", "/swagger-ui/**", "/v3/api-docs/**")
                // CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                
                // CSRF отключен для stateless JWT
                .csrf(csrf -> csrf.disable())
                
                // Stateless сессии (JWT)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                
                // Обработчик ошибок аутентификации
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )
                
                // Авторизация запросов
                .authorizeHttpRequests(auth -> auth
                        // Публичные auth endpoints (login, register, refresh)
                        .requestMatchers(PUBLIC_AUTH_ENDPOINTS).permitAll()
                        // Остальные публичные endpoints
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        // Auth endpoints требующие аутентификации (/me, /logout)
                        .requestMatchers(AUTH_PROTECTED_ENDPOINTS).authenticated()
                        // Только для админов
                        .requestMatchers(ADMIN_ENDPOINTS).hasRole("ADMIN")
                        // Все остальные требуют аутентификации
                        .anyRequest().authenticated()
                )
                
                // Провайдер аутентификации
                .authenticationProvider(authenticationProvider)
                
                // JWT фильтр перед стандартным фильтром аутентификации
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

            return http.build();
        }
    }

    // =====================
    // Development Configuration
    // =====================
    // В dev-режиме JWT всё ещё активен, но:
    // 1. Swagger и Actuator полностью открыты
    // 2. Логируется предупреждение о dev-режиме
    // 3. Можно использовать dev-токен для быстрого тестирования

    @Configuration
    @Profile("dev")
    public static class DevelopmentSecurityConfig {

        private static final org.slf4j.Logger logger = 
                org.slf4j.LoggerFactory.getLogger(DevelopmentSecurityConfig.class);

        // Публичные auth endpoints для dev
        private static final String[] DEV_PUBLIC_AUTH_ENDPOINTS = {
                ApiVersion.V1_AUTH + "/register",
                ApiVersion.V1_AUTH + "/login",
                ApiVersion.V1_AUTH + "/refresh",
                ApiVersion.V1_AUTH + "/validate"
        };
        
        private static final String[] DEV_PUBLIC_ENDPOINTS = {
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/v3/api-docs/**",
                "/actuator/**",  // Все actuator endpoints открыты в dev
                "/error",
                "/h2-console/**",  // Если используется H2 для тестов
                "/api/tournaments/public/**"
        };

        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
        private final AuthenticationProvider authenticationProvider;
        private final CorsConfigurationSource corsConfigurationSource;

        public DevelopmentSecurityConfig(
                JwtAuthenticationFilter jwtAuthenticationFilter,
                JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                AuthenticationProvider authenticationProvider,
                CorsConfigurationSource corsConfigurationSource
        ) {
            this.jwtAuthenticationFilter = jwtAuthenticationFilter;
            this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
            this.authenticationProvider = authenticationProvider;
            this.corsConfigurationSource = corsConfigurationSource;
            
            // Предупреждение при старте
            logger.warn("⚠️  ========================================");
            logger.warn("⚠️  DEVELOPMENT MODE ACTIVE");
            logger.warn("⚠️  JWT authentication is enabled but relaxed");
            logger.warn("⚠️  All Actuator endpoints are exposed");
            logger.warn("⚠️  DO NOT USE IN PRODUCTION!");
            logger.warn("⚠️  ========================================");
        }

        @Bean
        public SecurityFilterChain developmentSecurityFilterChain(HttpSecurity http) throws Exception {
            http
                // CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                
                // CSRF отключен для stateless JWT
                .csrf(csrf -> csrf.disable())
                
                // Stateless сессии (JWT)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                
                // Обработчик ошибок аутентификации
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )
                
                // Авторизация запросов (расширенный доступ для dev)
                .authorizeHttpRequests(auth -> auth
                        // Публичные auth endpoints
                        .requestMatchers(DEV_PUBLIC_AUTH_ENDPOINTS).permitAll()
                        // Расширенные публичные endpoints для dev
                        .requestMatchers(DEV_PUBLIC_ENDPOINTS).permitAll()
                        // Auth endpoints требующие аутентификации (/me, /logout)
                        .requestMatchers(AUTH_PROTECTED_ENDPOINTS).authenticated()
                        // Все остальные требуют аутентификации (JWT)
                        .anyRequest().authenticated()
                )
                
                // Провайдер аутентификации
                .authenticationProvider(authenticationProvider)
                
                // JWT фильтр
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                
                // Для H2 Console (если используется)
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

            return http.build();
        }
    }
}

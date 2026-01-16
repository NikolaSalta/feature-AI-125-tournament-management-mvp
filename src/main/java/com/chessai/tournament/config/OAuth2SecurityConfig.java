package com.chessai.tournament.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * OAuth2 Security Configuration for GitHub Authentication
 * 
 * This configuration enables GitHub OAuth2 login for development/MVP mode.
 * It works alongside the existing JWT-based API authentication.
 * 
 * Flow:
 * 1. User visits /oauth2/authorization/github
 * 2. Redirected to GitHub for authentication
 * 3. GitHub redirects back to /login/oauth2/code/github
 * 4. Spring Security processes the OAuth2 response
 * 5. User is authenticated and can access protected endpoints
 * 
 * @author Tournament Service Team
 * @since 1.0.0
 */
@Configuration
@EnableWebSecurity
@Profile({"dev", "oauth2"})
@Order(1) // Higher priority than JWT config
public class OAuth2SecurityConfig {

    /**
     * OAuth2 Security Filter Chain
     * 
     * This filter chain handles OAuth2 authentication flow and is applied
     * before the JWT-based security configuration.
     */
    @Bean
    public SecurityFilterChain oauth2FilterChain(HttpSecurity http) throws Exception {
        http
            // Apply to OAuth2 and web endpoints
            .securityMatcher("/oauth2/**", "/login/**", "/api/me", "/", "/home")
            
            // Disable CSRF for OAuth2 endpoints (GitHub handles CSRF protection)
            .csrf(AbstractHttpConfigurer::disable)
            
            // Configure authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/", "/home", "/public/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                
                // OAuth2 endpoints
                .requestMatchers("/oauth2/**", "/login/**").permitAll()
                
                // Protected endpoints require authentication
                .requestMatchers("/api/me").authenticated()
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            
            // Enable OAuth2 Login with default settings
            .oauth2Login(oauth2 -> oauth2
                .defaultSuccessUrl("/api/me", true)
                .failureUrl("/login?error")
            );

        return http.build();
    }
}
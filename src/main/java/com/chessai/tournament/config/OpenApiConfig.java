package com.chessai.tournament.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Конфигурация OpenAPI/Swagger для документации API.
 * 
 * Добавляет:
 * - Информацию о API (название, версия, описание)
 * - JWT Bearer аутентификацию
 * - Серверы для разных окружений
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private int serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server"),
                        new Server()
                                .url("https://api.tournament.example.com")
                                .description("Production Server")
                ))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT token. Get it from /api/v1/auth/login")
                        ));
    }

    private Info apiInfo() {
        return new Info()
                .title("Tournament Service API")
                .version("1.0.0")
                .description("""
                        REST API for Chess Tournament Management System.
                        
                        ## Authentication
                        
                        Most endpoints require JWT authentication. To get a token:
                        1. Register: `POST /api/v1/auth/register`
                        2. Login: `POST /api/v1/auth/login`
                        3. Use the returned `accessToken` in the Authorization header: `Bearer <token>`
                        
                        ## API Versioning
                        
                        API uses URL versioning: `/api/v1/...`
                        
                        ## Rate Limiting
                        
                        - Auth endpoints: 10 requests/minute
                        - API endpoints: 60 requests/minute
                        - Check `X-RateLimit-Remaining` header
                        """)
                .contact(new Contact()
                        .name("Tournament Service Team")
                        .email("support@tournament.example.com"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));
    }
}




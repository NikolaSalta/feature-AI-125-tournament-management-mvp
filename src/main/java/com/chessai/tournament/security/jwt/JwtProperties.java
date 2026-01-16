package com.chessai.tournament.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Конфигурация JWT токенов.
 * Значения загружаются из application.yml
 */
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * Секретный ключ для подписи токенов (минимум 256 бит для HS256)
     */
    private String secret;

    /**
     * Время жизни access-токена в миллисекундах (по умолчанию 30 минут)
     */
    private long accessTokenExpiration = 1800000;

    /**
     * Время жизни refresh-токена в миллисекундах (по умолчанию 7 дней)
     */
    private long refreshTokenExpiration = 604800000;

    /**
     * Издатель токена
     */
    private String issuer = "tournament-service";

    // Getters and Setters

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    public void setAccessTokenExpiration(long accessTokenExpiration) {
        this.accessTokenExpiration = accessTokenExpiration;
    }

    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    public void setRefreshTokenExpiration(long refreshTokenExpiration) {
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
}



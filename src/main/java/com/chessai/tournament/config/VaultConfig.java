package com.chessai.tournament.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import jakarta.annotation.PostConstruct;

/**
 * Конфигурация HashiCorp Vault.
 * 
 * Активируется только при включенном профиле "vault".
 * Проверяет подключение к Vault и логирует информацию о конфигурации.
 */
@Configuration
@Profile("vault")
@ConditionalOnProperty(name = "spring.cloud.vault.enabled", havingValue = "true", matchIfMissing = true)
public class VaultConfig {

    private static final Logger logger = LoggerFactory.getLogger(VaultConfig.class);

    @Value("${spring.cloud.vault.host:localhost}")
    private String vaultHost;

    @Value("${spring.cloud.vault.port:8200}")
    private int vaultPort;

    @Value("${spring.cloud.vault.scheme:http}")
    private String vaultScheme;

    @Value("${spring.cloud.vault.kv.backend:secret}")
    private String kvBackend;

    @Value("${spring.cloud.vault.kv.default-context:${spring.application.name}}")
    private String defaultContext;

    @Value("${spring.cloud.vault.authentication:TOKEN}")
    private String authMethod;

    /**
     * Инициализация конфигурации Vault.
     * Выводит информацию о подключении в логи.
     */
    @PostConstruct
    public void init() {
        logger.info("=".repeat(80));
        logger.info("HashiCorp Vault Configuration");
        logger.info("=".repeat(80));
        logger.info("Vault URL: {}://{}:{}", vaultScheme, vaultHost, vaultPort);
        logger.info("Authentication Method: {}", authMethod);
        logger.info("KV Backend: {}", kvBackend);
        logger.info("Default Context (Application): {}", defaultContext);
        logger.info("Vault Profile: ACTIVE");
        logger.info("=".repeat(80));
        
        if ("http".equalsIgnoreCase(vaultScheme)) {
            logger.warn("⚠️  WARNING: Vault is using HTTP (not HTTPS). This is insecure for production!");
        }
        
        if ("TOKEN".equalsIgnoreCase(authMethod)) {
            logger.warn("⚠️  WARNING: Using TOKEN authentication. Ensure VAULT_TOKEN is set securely!");
        }
    }

    /**
     * Возвращает полный путь к секретам приложения в Vault
     */
    public String getSecretPath() {
        return String.format("%s/data/%s", kvBackend, defaultContext);
    }

    /**
     * Возвращает путь к секретам для конкретного профиля
     */
    public String getSecretPath(String profile) {
        return String.format("%s/data/%s/%s", kvBackend, defaultContext, profile);
    }
}




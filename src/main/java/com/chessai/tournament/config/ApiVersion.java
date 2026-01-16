package com.chessai.tournament.config;

/**
 * Константы версий API.
 * 
 * Использование версионирования API позволяет:
 * - Поддерживать обратную совместимость
 * - Вносить breaking changes в новых версиях
 * - Постепенно мигрировать клиентов на новые версии
 * 
 * Формат URL: /api/v{version}/resource
 * Пример: /api/v1/auth/login
 */
public final class ApiVersion {

    private ApiVersion() {
        // Utility class
    }

    /**
     * Базовый путь API
     */
    public static final String API_BASE = "/api";

    /**
     * API версия 1 (текущая стабильная)
     */
    public static final String V1 = API_BASE + "/v1";

    /**
     * API версия 2 (будущая)
     */
    public static final String V2 = API_BASE + "/v2";

    /**
     * Текущая версия API (для использования по умолчанию)
     */
    public static final String CURRENT = V1;

    // =====================
    // Полные пути к ресурсам
    // =====================

    /**
     * Путь к auth endpoints (v1)
     */
    public static final String V1_AUTH = V1 + "/auth";

    /**
     * Путь к tournaments endpoints (v1)
     */
    public static final String V1_TOURNAMENTS = V1 + "/tournaments";

    /**
     * Путь к players endpoints (v1)
     */
    public static final String V1_PLAYERS = V1 + "/players";

    /**
     * Путь к admin endpoints (v1)
     */
    public static final String V1_ADMIN = V1 + "/admin";

    /**
     * Путь к users endpoints (v1)
     */
    public static final String V1_USERS = V1 + "/users";
}




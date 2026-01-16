package com.chessai.tournament.security.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Фильтр для применения Rate Limiting к HTTP-запросам.
 * 
 * Применяет разные лимиты для разных типов эндпоинтов:
 * - /api/auth/** - строгий лимит для защиты от брутфорса
 * - /api/** - стандартный лимит для API
 * - остальные - общий лимит
 * 
 * Отключается для тестов через spring.profiles.active=test
 */
/**
 * SECURITY: Порядок фильтра = 10 (после JwtAuthenticationFilter, который имеет порядок 1).
 * Это позволяет getRateLimitKey() корректно получить userId из SecurityContext.
 * 
 * HIGHEST_PRECEDENCE был бы Integer.MIN_VALUE, что означало бы выполнение
 * ДО JWT-фильтра, и тогда SecurityContext был бы пустым.
 */
@Component
@Order(10)
@org.springframework.context.annotation.Profile("!test")
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);
    private final RateLimitingService rateLimitingService;
    private final ObjectMapper objectMapper;

    public RateLimitingFilter(RateLimitingService rateLimitingService) {
        this.rateLimitingService = rateLimitingService;

        // Настраиваем ObjectMapper с поддержкой Java 8 Date/Time API
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String clientIp = getClientIp(request);
        String path = request.getRequestURI();
        
        // SECURITY: Используем комбинацию IP + userId для rate limiting
        // Если пользователь авторизован, используем userId для более точного отслеживания
        // Если нет, используем IP (но это может быть подделано за NAT)
        String rateLimitKey = getRateLimitKey(clientIp);

        RateLimitingService.RateLimitType limitType = determineLimitType(path);

        if (!rateLimitingService.tryConsume(rateLimitKey, limitType)) {
            logger.warn("Rate limit exceeded for key: {} (IP: {}) on path: {}", rateLimitKey, clientIp, path);
            sendRateLimitExceededResponse(response, request.getRequestURI());
            return;
        }

        // Добавляем заголовки с информацией о лимитах
        long remaining = rateLimitingService.getAvailableTokens(rateLimitKey, limitType);
        response.setHeader("X-RateLimit-Limit", String.valueOf(limitType.getCapacity()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));

        filterChain.doFilter(request, response);
    }

    /**
     * Получает ключ для rate limiting - комбинация IP + userId (если авторизован).
     * 
     * SECURITY: Если пользователь авторизован (JWT token), используем userId.
     * Это позволяет избежать проблемы где все пользователи за NAT кажутся одним IP.
     * Если нет - используем IP, но помним что это может быть подделано.
     */
    private String getRateLimitKey(String clientIp) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
            // Для авторизованных пользователей используем userId
            return "user:" + auth.getName();
        }
        // Для неавторизованных используем IP
        return "ip:" + clientIp;
    }

    /**
     * Определяет тип лимита на основе пути запроса.
     * Поддерживает версионированные API (/api/v1/..., /api/v2/...).
     */
    private RateLimitingService.RateLimitType determineLimitType(String path) {
        // Auth endpoints (любая версия API)
        if (path.matches("/api/v\\d+/auth.*") || path.startsWith("/api/auth")) {
            return RateLimitingService.RateLimitType.AUTH;
        }
        // Все API endpoints
        if (path.startsWith("/api")) {
            return RateLimitingService.RateLimitType.API;
        }
        return RateLimitingService.RateLimitType.GENERAL;
    }

    /**
     * Извлекает IP-адрес клиента с учетом прокси.
     * 
     * ВАЖНО: Используем ПОСЛЕДНИЙ IP в X-Forwarded-For,
     * т.к. первый может быть подделан клиентом.
     * Последний IP добавляется доверенным прокси.
     * 
     * SECURITY NOTE: X-Real-IP и X-Forwarded-For заголовки могут быть
     * подделаны, если приложение НЕ находится за доверенным прокси.
     * В production окружении убедитесь, что:
     * 1. Приложение доступно ТОЛЬКО через прокси (nginx, load balancer)
     * 2. Прокси настроен на перезапись этих заголовков
     * 3. Прямой доступ к приложению заблокирован файрволом
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Берем ПОСЛЕДНИЙ IP из списка (добавлен прокси, не подделаешь)
            String[] ips = xForwardedFor.split(",");
            return ips[ips.length - 1].trim();
        }

        // SECURITY: X-Real-IP доверяем только если запрос пришел через прокси
        // В production это должно контролироваться на уровне инфраструктуры
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * Отправляет ответ о превышении лимита
     */
    private void sendRateLimitExceededResponse(HttpServletResponse response, String path) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(429); // Too Many Requests

        Map<String, Object> body = new HashMap<>();
        body.put("status", 429);
        body.put("error", "Too Many Requests");
        body.put("message", "Rate limit exceeded. Please try again later.");
        body.put("path", path);
        body.put("timestamp", LocalDateTime.now().toString());

        objectMapper.writeValue(response.getOutputStream(), body);
    }

    /**
     * Пропускаем статические ресурсы и actuator
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/health") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.endsWith(".css") ||
                path.endsWith(".js") ||
                path.endsWith(".ico");
    }
}

package com.chessai.tournament.security.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Сервис для Rate Limiting.
 * Защищает API от DDoS-атак и злоупотреблений.
 * 
 * Использует алгоритм Token Bucket для контроля частоты запросов.
 * Caffeine cache с TTL предотвращает утечку памяти.
 * 
 * Отключается для тестов через spring.profiles.active=test
 */
@Service
@org.springframework.context.annotation.Profile("!test")
public class RateLimitingService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingService.class);

    /**
     * TTL кэш bucket'ов с автоматической очисткой неиспользуемых записей.
     * Записи истекают через 10 минут неактивности.
     * Максимум 10000 записей для защиты от OOM.
     */
    private final Cache<String, Bucket> bucketCache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .maximumSize(10_000)
            .recordStats()
            .build();

    /**
     * Конфигурация для разных типов ограничений
     */
    public enum RateLimitType {
        /**
         * Общие запросы: 100 запросов в минуту
         */
        GENERAL(100, Duration.ofMinutes(1)),

        /**
         * Аутентификация: 10 попыток в минуту (защита от брутфорса)
         */
        AUTH(10, Duration.ofMinutes(1)),

        /**
         * API endpoints: 60 запросов в минуту
         */
        API(60, Duration.ofMinutes(1)),

        /**
         * Тяжелые операции: 10 в минуту
         */
        HEAVY(10, Duration.ofMinutes(1));

        private final int capacity;
        private final Duration refillPeriod;

        RateLimitType(int capacity, Duration refillPeriod) {
            this.capacity = capacity;
            this.refillPeriod = refillPeriod;
        }

        public int getCapacity() {
            return capacity;
        }

        public Duration getRefillPeriod() {
            return refillPeriod;
        }
    }

    /**
     * Проверяет и потребляет токен.
     * 
     * @param key  уникальный ключ (обычно IP-адрес или userId)
     * @param type тип ограничения
     * @return true если запрос разрешен, false если лимит превышен
     */
    public boolean tryConsume(String key, RateLimitType type) {
        String bucketKey = type.name() + ":" + key;
        Bucket bucket = bucketCache.get(bucketKey, k -> createBucket(type));
        return bucket.tryConsume(1);
    }

    /**
     * Проверяет доступность токенов без потребления
     */
    public boolean isAllowed(String key, RateLimitType type) {
        String bucketKey = type.name() + ":" + key;
        Bucket bucket = bucketCache.getIfPresent(bucketKey);
        if (bucket == null) {
            return true;
        }
        return bucket.getAvailableTokens() > 0;
    }

    /**
     * Получает количество оставшихся токенов
     */
    public long getAvailableTokens(String key, RateLimitType type) {
        String bucketKey = type.name() + ":" + key;
        Bucket bucket = bucketCache.getIfPresent(bucketKey);
        if (bucket == null) {
            return type.getCapacity();
        }
        return bucket.getAvailableTokens();
    }

    /**
     * Сбрасывает лимит для ключа
     */
    public void resetLimit(String key, RateLimitType type) {
        String bucketKey = type.name() + ":" + key;
        bucketCache.invalidate(bucketKey);
    }

    /**
     * Создает новый bucket с заданными ограничениями
     */
    private Bucket createBucket(RateLimitType type) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(type.getCapacity())
                .refillIntervally(type.getCapacity(), type.getRefillPeriod())
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Очищает кэш (для тестов или периодической очистки)
     */
    public void clearCache() {
        bucketCache.invalidateAll();
    }

    /**
     * Получает размер кэша
     */
    public long getCacheSize() {
        return bucketCache.estimatedSize();
    }

    /**
     * Получает статистику кэша для мониторинга
     */
    public String getCacheStats() {
        return bucketCache.stats().toString();
    }
}

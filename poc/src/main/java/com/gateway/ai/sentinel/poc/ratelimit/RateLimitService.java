package com.gateway.ai.sentinel.poc.ratelimit;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis tabanlı Token Bucket algoritması ile API rate limiting servisi.
 *
 * <p>Her kullanıcı için ayrı bir Redis key oluşturulur:
 * {@code rate_limit:{userId}}
 *
 * <p>İlk istek geldiğinde key oluşturulur ve TTL ayarlanır.
 * Her istek INCR ile artırılır. Eşik aşılırsa {@link RateLimitException} fırlatılır.
 */
@Service
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);
    private static final String KEY_PREFIX = "rate_limit:";

    private final RedisTemplate<String, String> redisTemplate;
    private final int maxRequests;
    private final int windowSeconds;

    public RateLimitService(
            RedisTemplate<String, String> redisTemplate,
            @Value("${sentinel.rate-limit.max-requests:10}") int maxRequests,
            @Value("${sentinel.rate-limit.window-seconds:60}") int windowSeconds) {
        this.redisTemplate = redisTemplate;
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
    }

    /**
     * Belirtilen kullanıcı için rate limit kontrolü yapar.
     *
     * @param userId Kontrol edilecek kullanıcı kimliği
     * @throws RateLimitException İzin verilen istek sayısı aşıldığında
     */
    public void checkRateLimit(String userId) {
        String key = KEY_PREFIX + userId;

        Long requestCount = redisTemplate.opsForValue().increment(key);

        // İlk istek ise TTL'i ayarla (Token Bucket penceresi)
        if (requestCount != null && requestCount == 1) {
            redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
        }

        if (requestCount != null && requestCount > maxRequests) {
            log.warn("Rate limit exceeded for userId={}. Count={}, Max={}", userId, requestCount, maxRequests);
            throw new RateLimitException(
                    "Rate limit exceeded. Maximum %d requests per %d seconds allowed.".formatted(maxRequests, windowSeconds));
        }

        log.debug("Rate limit check passed for userId={}. Count={}/{}", userId, requestCount, maxRequests);
    }

    /**
     * Kullanıcının mevcut istek sayısını döner (test ve izleme amaçlı).
     *
     * @param userId Kullanıcı kimliği
     * @return Mevcut istek sayısı veya 0
     */
    public long getCurrentCount(String userId) {
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + userId);
        return value != null ? Long.parseLong(value) : 0L;
    }
}

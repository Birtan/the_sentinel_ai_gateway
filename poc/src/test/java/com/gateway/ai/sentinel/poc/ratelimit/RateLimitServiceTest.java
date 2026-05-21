package com.gateway.ai.sentinel.poc.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * {@link RateLimitService} unit testleri.
 *
 * <p>Spring context veya Redis bağlantısı gerektirmez;
 * {@link RedisTemplate} Mockito ile mock'lanır.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitService Unit Tests")
class RateLimitServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RateLimitService rateLimitService;

    private static final int MAX_REQUESTS = 5;
    private static final int WINDOW_SECONDS = 60;
    private static final String TEST_USER_ID = "test-user-123";

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        rateLimitService = new RateLimitService(redisTemplate, MAX_REQUESTS, WINDOW_SECONDS);
    }

    @Test
    @DisplayName("İlk istek için TTL set edilmeli")
    void shouldSetTtlOnFirstRequest() {
        // Arrange
        when(valueOperations.increment("rate_limit:" + TEST_USER_ID)).thenReturn(1L);

        // Act
        rateLimitService.checkRateLimit(TEST_USER_ID);

        // Assert — İlk istek olduğu için expire çağrılmalı
        verify(redisTemplate, times(1)).expire("rate_limit:" + TEST_USER_ID, Duration.ofSeconds(WINDOW_SECONDS));
    }

    @Test
    @DisplayName("Sonraki isteklerde TTL tekrar set edilmemeli")
    void shouldNotSetTtlOnSubsequentRequests() {
        // Arrange
        when(valueOperations.increment("rate_limit:" + TEST_USER_ID)).thenReturn(2L);

        // Act
        rateLimitService.checkRateLimit(TEST_USER_ID);

        // Assert — 2. istek olduğu için expire çağrılmamalı
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("Limit dahilinde istek geçmeli")
    void shouldPassWhenUnderLimit() {
        // Arrange
        when(valueOperations.increment("rate_limit:" + TEST_USER_ID)).thenReturn((long) MAX_REQUESTS);

        // Act & Assert — Exception fırlatılmamalı
        rateLimitService.checkRateLimit(TEST_USER_ID);
    }

    @Test
    @DisplayName("Limit aşılınca RateLimitException fırlatılmalı")
    void shouldThrowRateLimitExceptionWhenLimitExceeded() {
        // Arrange — Limit + 1 istek geldi
        when(valueOperations.increment("rate_limit:" + TEST_USER_ID)).thenReturn((long) (MAX_REQUESTS + 1));

        // Act & Assert
        assertThatThrownBy(() -> rateLimitService.checkRateLimit(TEST_USER_ID))
                .isInstanceOf(RateLimitException.class)
                .hasMessageContaining("Rate limit exceeded");
    }

    @Test
    @DisplayName("getCurrentCount mevcut sayıyı doğru döndürmeli")
    void shouldReturnCurrentCountCorrectly() {
        // Arrange
        when(valueOperations.get("rate_limit:" + TEST_USER_ID)).thenReturn("7");

        // Act
        long count = rateLimitService.getCurrentCount(TEST_USER_ID);

        // Assert
        assertThat(count).isEqualTo(7L);
    }

    @Test
    @DisplayName("Key yoksa getCurrentCount sıfır döndürmeli")
    void shouldReturnZeroWhenKeyNotExists() {
        // Arrange
        when(valueOperations.get("rate_limit:" + TEST_USER_ID)).thenReturn(null);

        // Act
        long count = rateLimitService.getCurrentCount(TEST_USER_ID);

        // Assert
        assertThat(count).isZero();
    }
}

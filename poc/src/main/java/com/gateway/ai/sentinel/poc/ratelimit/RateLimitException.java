package com.gateway.ai.sentinel.poc.ratelimit;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Bir kullanıcı rate limit eşiğini aştığında fırlatılır.
 * {@link com.gateway.ai.sentinel.poc.exception.GlobalExceptionHandler} tarafından
 * 429 Too Many Requests HTTP yanıtına dönüştürülür.
 */
@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class RateLimitException extends RuntimeException {

    public RateLimitException(String message) {
        super(message);
    }
}

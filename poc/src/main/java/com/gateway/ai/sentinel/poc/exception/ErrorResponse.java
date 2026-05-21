package com.gateway.ai.sentinel.poc.exception;

import java.time.Instant;

/**
 * API hata yanıtı için standart model.
 *
 * @param status    HTTP durum kodu
 * @param message   Hata mesajı
 * @param timestamp Hatanın oluştuğu zaman
 */
public record ErrorResponse(int status, String message, Instant timestamp) {}

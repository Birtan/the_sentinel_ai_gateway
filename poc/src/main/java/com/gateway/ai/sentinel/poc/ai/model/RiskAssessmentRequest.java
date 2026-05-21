package com.gateway.ai.sentinel.poc.ai.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Risk analizi API endpoint'ine gelen istek modeli.
 *
 * @param payload Analiz edilecek ham metin (işlem detayları, davranış logları vb.)
 */
public record RiskAssessmentRequest(
        @NotBlank(message = "Payload boş olamaz")
        @Size(min = 10, max = 5000, message = "Payload 10 ile 5000 karakter arasında olmalıdır")
        String payload) {}

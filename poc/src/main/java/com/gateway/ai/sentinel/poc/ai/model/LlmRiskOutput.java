package com.gateway.ai.sentinel.poc.ai.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * LLM'den gelen ham risk değerlendirme çıktısı.
 * Spring AI'ın BeanOutputConverter'ı tarafından JSON'dan deserialize edilir.
 *
 * @param riskScore  1-10 arası risk skoru (1: çok düşük, 10: kritik)
 * @param category   Risk kategorisi (FRAUD, PHISHING, MALWARE, SPAM, BENIGN)
 * @param reason     Risk skorunu açıklayan kısa gerekçe
 */
public record LlmRiskOutput(
        @JsonProperty("riskScore") int riskScore,
        @JsonProperty("category") String category,
        @JsonProperty("reason") String reason) {}

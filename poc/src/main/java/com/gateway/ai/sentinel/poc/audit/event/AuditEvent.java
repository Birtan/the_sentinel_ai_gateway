package com.gateway.ai.sentinel.poc.audit.event;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Kafka {@code sentinel.audit.events} topic'ine publish edilen denetim olayı.
 *
 * <p>Her başarılı risk analizi sonrasında bir {@code AuditEvent} üretilir.
 * JSON olarak serileştirilir ve Kafka topic'e yazılır.
 */
public class AuditEvent {

    /** Analiz isteğinin benzersiz kimliği. Kafka partition key olarak kullanılır. */
    @JsonProperty("requestId")
    private String requestId;

    /** İsteği yapan kullanıcının Keycloak subject ID'si. */
    @JsonProperty("userId")
    private String userId;

    /** LLM tarafından hesaplanan risk skoru (1-10). */
    @JsonProperty("riskScore")
    private int riskScore;

    /** Risk kategorisi (FRAUD, PHISHING vb.). */
    @JsonProperty("category")
    private String category;

    /** Risk skorunu açıklayan kısa gerekçe. */
    @JsonProperty("reason")
    private String reason;

    /** Analizin gerçekleştiği UTC zaman damgası. */
    @JsonProperty("timestamp")
    private Instant timestamp;

    /** Analiz edilen payload'ın ilk 100 karakteri (audit trail için). */
    @JsonProperty("payloadSnippet")
    private String payloadSnippet;

    // ===== No-arg constructor for Jackson deserialization =====
    public AuditEvent() {}

    // ===== All-arg constructor =====
    private AuditEvent(Builder builder) {
        this.requestId = builder.requestId;
        this.userId = builder.userId;
        this.riskScore = builder.riskScore;
        this.category = builder.category;
        this.reason = builder.reason;
        this.timestamp = builder.timestamp;
        this.payloadSnippet = builder.payloadSnippet;
    }

    // ===== Builder =====
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String requestId;
        private String userId;
        private int riskScore;
        private String category;
        private String reason;
        private Instant timestamp;
        private String payloadSnippet;

        private Builder() {}

        public Builder requestId(String requestId) { this.requestId = requestId; return this; }
        public Builder userId(String userId) { this.userId = userId; return this; }
        public Builder riskScore(int riskScore) { this.riskScore = riskScore; return this; }
        public Builder category(String category) { this.category = category; return this; }
        public Builder reason(String reason) { this.reason = reason; return this; }
        public Builder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }
        public Builder payloadSnippet(String payloadSnippet) { this.payloadSnippet = payloadSnippet; return this; }

        public AuditEvent build() { return new AuditEvent(this); }
    }

    // ===== Getters & Setters (for Jackson) =====
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public int getRiskScore() { return riskScore; }
    public void setRiskScore(int riskScore) { this.riskScore = riskScore; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getPayloadSnippet() { return payloadSnippet; }
    public void setPayloadSnippet(String payloadSnippet) { this.payloadSnippet = payloadSnippet; }

    @Override
    public String toString() {
        return "AuditEvent{requestId='" + requestId + "', userId='" + userId +
               "', riskScore=" + riskScore + ", category='" + category + "'}";
    }
}

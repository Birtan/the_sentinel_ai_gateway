package com.gateway.ai.sentinel.poc.ai.model;

import java.time.Instant;

/**
 * Risk analizi API endpoint'inin dönüş modeli.
 * LLM çıktısı + servis tarafından eklenen metadata içerir.
 *
 * @param requestId Her istek için üretilen benzersiz UUID
 * @param riskScore 1-10 arası risk skoru
 * @param category  Risk kategorisi
 * @param reason    Risk gerekçesi
 * @param timestamp Analizin gerçekleştiği zaman damgası
 */
public record RiskAssessmentResponse(
        String requestId,
        int riskScore,
        String category,
        String reason,
        Instant timestamp) {

    /** Builder desteği için statik factory metodu. */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String requestId;
        private int riskScore;
        private String category;
        private String reason;
        private Instant timestamp;

        private Builder() {
        }

        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder riskScore(int riskScore) {
            this.riskScore = riskScore;
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public RiskAssessmentResponse build() {
            return new RiskAssessmentResponse(requestId, riskScore, category, reason, timestamp);
        }
    }
}

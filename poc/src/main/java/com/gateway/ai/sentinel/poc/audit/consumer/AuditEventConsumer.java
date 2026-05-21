package com.gateway.ai.sentinel.poc.audit.consumer;

import com.gateway.ai.sentinel.poc.audit.event.AuditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * {@code sentinel.audit.events} topic'ini dinleyen denetim tüketicisi.
 *
 * <p>
 * Hata toleransı için {@link com.gateway.ai.sentinel.poc.config.KafkaConfig}
 * üzerinden
 * yapılandırılmış {@code DefaultErrorHandler} ve
 * {@code DeadLetterPublishingRecoverer} kullanılır.
 * 3 deneme sonrası başarısız mesajlar {@code sentinel.audit.events.DLT}
 * topic'ine taşınır.
 */
@Component
public class AuditEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditEventConsumer.class);

    /**
     * Başarılı risk analizi olaylarını işler.
     *
     * @param event Kafka'dan tüketilen denetim olayı
     */
    @KafkaListener(topics = "sentinel.audit.events", groupId = "sentinel-audit-consumer")
    public void consume(AuditEvent event) {
        log.info(
                "✅ Audit event received — requestId={}, userId={}, riskScore={}, category={}",
                event.getRequestId(),
                event.getUserId(),
                event.getRiskScore(),
                event.getCategory());

    }

    /**
     * Dead Letter Topic'ten başarısız mesajları işler.
     *
     * @param event DLT'den tüketilen başarısız denetim olayı
     */
    @KafkaListener(topics = "sentinel.audit.events.DLT", groupId = "sentinel-audit-dlt-consumer")
    public void consumeDeadLetter(AuditEvent event) {
        log.error(
                "💀 Dead letter event received — requestId={}, userId={}, riskScore={}. Manual intervention required.",
                event.getRequestId(),
                event.getUserId(),
                event.getRiskScore());
    }
}

package com.gateway.ai.sentinel.poc.audit.producer;

import com.gateway.ai.sentinel.poc.audit.event.AuditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * {@code sentinel.audit.events} Kafka topic'ine denetim olayları yayınlayan üretici.
 *
 * <p>Tüm publish işlemleri asenkron çalışır. Başarısız gönderimler loglanır;
 * Dead Letter Topic (DLT) yönetimi consumer tarafında {@link com.gateway.ai.sentinel.poc.config.KafkaConfig}
 * üzerinden yapılır.
 */
@Service
public class AuditEventProducer {

    private static final Logger log = LoggerFactory.getLogger(AuditEventProducer.class);

    static final String TOPIC = "sentinel.audit.events";

    private final KafkaTemplate<String, AuditEvent> kafkaTemplate;

    public AuditEventProducer(KafkaTemplate<String, AuditEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Belirtilen denetim olayını Kafka topic'ine asenkron olarak yayınlar.
     * {@code requestId} partition key olarak kullanılarak mesaj sıralaması sağlanır.
     *
     * @param event Yayınlanacak denetim olayı
     */
    public void publishAuditEvent(AuditEvent event) {
        kafkaTemplate
                .send(TOPIC, event.getRequestId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error(
                                "Audit event publish FAILED: requestId={}, error={}",
                                event.getRequestId(),
                                ex.getMessage(),
                                ex);
                    } else {
                        log.debug(
                                "Audit event published: requestId={}, topic={}, partition={}, offset={}",
                                event.getRequestId(),
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}

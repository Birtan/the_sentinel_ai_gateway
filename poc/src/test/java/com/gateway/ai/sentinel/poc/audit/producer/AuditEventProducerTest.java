package com.gateway.ai.sentinel.poc.audit.producer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import com.gateway.ai.sentinel.poc.audit.event.AuditEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

/**
 * {@link AuditEventProducer} unit testleri.
 *
 * <p>{@link KafkaTemplate} Mockito ile mock'lanır; gerçek Kafka bağlantısı gerekmez.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuditEventProducer Unit Tests")
class AuditEventProducerTest {

    @Mock
    private KafkaTemplate<String, AuditEvent> kafkaTemplate;

    private AuditEventProducer auditEventProducer;

    @BeforeEach
    void setUp() {
        auditEventProducer = new AuditEventProducer(kafkaTemplate);
    }

    @Test
    @DisplayName("AuditEvent doğru topic ve key ile publish edilmeli")
    void shouldPublishAuditEventToCorrectTopic() {
        // Arrange
        AuditEvent event = buildSampleAuditEvent("req-001");
        ProducerRecord<String, AuditEvent> producerRecord = new ProducerRecord<>(AuditEventProducer.TOPIC, "req-001", event);
        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition(AuditEventProducer.TOPIC, 0), 0L, 0, 0L, 0, 0);
        SendResult<String, AuditEvent> sendResult = new SendResult<>(producerRecord, metadata);

        when(kafkaTemplate.send(AuditEventProducer.TOPIC, "req-001", event))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        // Act
        auditEventProducer.publishAuditEvent(event);

        // Assert
        verify(kafkaTemplate, times(1)).send(AuditEventProducer.TOPIC, "req-001", event);
    }

    @Test
    @DisplayName("Kafka hatası durumunda exception fırlatılmamalı (async)")
    void shouldHandleKafkaFailureGracefully() {
        // Arrange
        AuditEvent event = buildSampleAuditEvent("req-002");
        CompletableFuture<SendResult<String, AuditEvent>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka connection refused"));

        when(kafkaTemplate.send(any(), any(), any())).thenReturn(failedFuture);

        // Act & Assert — async hata loglanmalı, exception fırlatılmamalı
        auditEventProducer.publishAuditEvent(event);

        verify(kafkaTemplate, times(1)).send(AuditEventProducer.TOPIC, "req-002", event);
    }

    @Test
    @DisplayName("RequestId partition key olarak kullanılmalı")
    void shouldUseRequestIdAsPartitionKey() {
        // Arrange
        String expectedRequestId = "unique-request-id-xyz";
        AuditEvent event = buildSampleAuditEvent(expectedRequestId);
        when(kafkaTemplate.send(any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        // Act
        auditEventProducer.publishAuditEvent(event);

        // Assert
        verify(kafkaTemplate).send(AuditEventProducer.TOPIC, expectedRequestId, event);
    }

    private AuditEvent buildSampleAuditEvent(String requestId) {
        return AuditEvent.builder()
                .requestId(requestId)
                .userId("user-123")
                .riskScore(7)
                .category("FRAUD")
                .reason("Suspicious activity detected")
                .timestamp(Instant.now())
                .payloadSnippet("test payload snippet")
                .build();
    }
}

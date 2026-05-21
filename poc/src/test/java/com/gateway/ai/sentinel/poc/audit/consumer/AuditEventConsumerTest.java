package com.gateway.ai.sentinel.poc.audit.consumer;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Instant;

import com.gateway.ai.sentinel.poc.audit.event.AuditEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AuditEventConsumer Unit Tests")
class AuditEventConsumerTest {

    private final AuditEventConsumer auditEventConsumer = new AuditEventConsumer();

    @Test
    @DisplayName("consume metodu hata fırlatmadan audit event'i loglamalı")
    void shouldConsumeEventWithoutException() {
        // Arrange
        AuditEvent event = AuditEvent.builder()
                .requestId("req-123")
                .userId("user-456")
                .riskScore(5)
                .category("SUSPICIOUS")
                .reason("Test reason")
                .timestamp(Instant.now())
                .payloadSnippet("Test payload")
                .build();

        // Act & Assert
        assertThatCode(() -> auditEventConsumer.consume(event))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("consumeDeadLetter metodu hata fırlatmadan dead letter event'i loglamalı")
    void shouldConsumeDeadLetterWithoutException() {
        // Arrange
        AuditEvent event = AuditEvent.builder()
                .requestId("req-dlt-123")
                .userId("user-dlt-456")
                .riskScore(9)
                .category("FRAUD")
                .reason("Test DLT reason")
                .timestamp(Instant.now())
                .payloadSnippet("Test DLT payload")
                .build();

        // Act & Assert
        assertThatCode(() -> auditEventConsumer.consumeDeadLetter(event))
                .doesNotThrowAnyException();
    }
}

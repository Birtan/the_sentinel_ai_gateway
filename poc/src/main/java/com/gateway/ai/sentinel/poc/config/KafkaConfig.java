package com.gateway.ai.sentinel.poc.config;

import com.gateway.ai.sentinel.poc.audit.event.AuditEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Map;

/**
 * Kafka konfigürasyonu.
 *
 * <p>
 * Temel özellikler:
 * <ul>
 * <li>İdempotent producer (exactly-once semantics)</li>
 * <li>JSON serialization / deserialization</li>
 * <li>{@link ErrorHandlingDeserializer} ile poison pill koruması</li>
 * <li>{@link DeadLetterPublishingRecoverer} ile 3 deneme sonrası DLT
 * yönlendirmesi</li>
 * </ul>
 *
 * <p>
 * Spring Boot 4.x: {@link KafkaProperties} paketi
 * {@code org.springframework.boot.kafka.autoconfigure} olarak taşınmıştır.
 * Jackson paketi {@code tools.jackson} olarak değiştiğinden Spring Kafka'nın
 * {@code JsonSerializer/Deserializer}'ı application.yml üzerinden
 * yapılandırılır.
 */
@Configuration
public class KafkaConfig {

    // ===== TOPICS =====

    @Bean
    public NewTopic auditEventsTopic() {
        return TopicBuilder.name("sentinel.audit.events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic auditEventsDltTopic() {
        return TopicBuilder.name("sentinel.audit.events.DLT")
                .partitions(1)
                .replicas(1)
                .build();
    }

    // ===== PRODUCER =====

    @Bean
    public ProducerFactory<String, AuditEvent> auditEventProducerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> props = kafkaProperties.buildProducerProperties();
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        // Value serializer: application.yml'dan spring.kafka.producer.value-serializer
        // alınır
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, AuditEvent> kafkaTemplate(
            ProducerFactory<String, AuditEvent> auditEventProducerFactory) {
        return new KafkaTemplate<>(auditEventProducerFactory);
    }

    // ===== CONSUMER =====

    @Bean
    public ConsumerFactory<String, AuditEvent> auditEventConsumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> props = kafkaProperties.buildConsumerProperties();
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        // Delegate deserializer ve trusted packages: application.yml'dan alınır
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, AuditEvent> template) {
        DeadLetterPublishingRecoverer dltRecoverer = new DeadLetterPublishingRecoverer(
                template,
                (failedEvent, exception) -> new TopicPartition("sentinel.audit.events.DLT", 0));

        // 1 saniye bekleme, maksimum 3 deneme
        return new DefaultErrorHandler(dltRecoverer, new FixedBackOff(1000L, 3));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, AuditEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, AuditEvent> auditEventConsumerFactory,
            DefaultErrorHandler errorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, AuditEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(auditEventConsumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}

package com.gateway.ai.sentinel.poc;

import com.gateway.ai.sentinel.poc.ai.service.RiskAnalysisService;
import com.gateway.ai.sentinel.poc.audit.producer.AuditEventProducer;
import com.gateway.ai.sentinel.poc.config.TestContainerConfig;
import com.gateway.ai.sentinel.poc.ratelimit.RateLimitService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring application context yükleme smoke testi.
 *
 * <p>Tüm bean'lerin başarıyla oluşturulduğunu doğrular.
 * Spring AI mock'lanır; Redis ve Kafka gerçek container'larla test edilir.
 */
@SpringBootTest
@Import(TestContainerConfig.class)
@ActiveProfiles("test")
@DisplayName("Application Context Smoke Test")
@Disabled("Docker daemon is not available in the build/test environment")
class PocApplicationTests {

    @MockitoBean
    private RiskAnalysisService riskAnalysisService;

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private AuditEventProducer auditEventProducer;

    @Test
    @DisplayName("Spring context başarıyla yüklenmeli")
    void contextLoads() {
        assertThat(rateLimitService).isNotNull();
        assertThat(auditEventProducer).isNotNull();
    }
}
